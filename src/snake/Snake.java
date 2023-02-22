package snake;

/*
Name:    Joel Anglister
Date:    12/10/18
What I learned: How to use multi-threading with a class that implements Runnable, then creating a thread with an object of that class.
                You have to use multi-threading because you can't update the board and read from the keyboard at the same time, so you
                can use one thread for one purpose and another thread for another purpose. Also, I learned how to use a switch-case block.
                Lastly, I learned how to make a key listener useing the KeyListener interface. It has 3 methods: one that reads when a key
                is pressed, one when it is typed, and one when it is released. I put all of my code in the keyPressed() method so that it
                runs as soon as the key is pressed, giving the player more control over the timing of the snake.

                12/13/18: I just finished writing an AI for the snake program. My first idea was to create a third thread that would handle
                the snake's brain, and I tried it, but I was experiencing some strange bugs. It seems to me that the threads would run at
                very slightly different times, and this caused the snake to move before the optimal direction could be chosen by the brain,
                resulting in premature deaths and disorganized system outputs. In order to fix this problem, I moved the snake's brain to
                the mover thread and added a boolean to keep track of whether the AI should be in control or not. If so, the brain makes a
                decision on where to move next, and then the snake moves afterwards. This eliminates the risk of the brain and the mover
                not operating at the same time. Second of all, I commented the line of the for loop in the snake's brain because it is
                paramount in making accurate "defenseive manouvers". The problem was that the first if statements would change the values
                of dr and dc, but the defensive statements were written with the assumption that they were not changed. This confused the
                snake and enabled it to randomly kill itself. The advantage of checking it twice, which is what the loop does, is that it
                bypasses the restriction set by the previous statements and enables the snake to make the right decision. At this point,
                the snake can only really "see" one move ahead.
                
                12/15/18: At this point, the AI now senses when it's about to die, and then makes a decision on where to go next based on
                the empty space in each direction. It looks in every direction, guages how much area it has to move around in that direction,
                then picks the direction with the most area in which to move around. It can still trap itself, though, and this is (I believe)
                the only way it can currently die.
*/
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

public class Snake extends JPanel
{
   public static final int SIZE = 30;
   public static final int SPEED = 2000 / SIZE;
   public static int aiSpeed = 1000 / SIZE;
   public Thread mover = null;
   public boolean ai = true;
   
   private int dr = 0, dc = 0;
   private MyButton[][] board;
   private MyButton food;
   private LinkedList<MyButton> snake = new LinkedList<MyButton>();
   private LinkedList<MyButton> checked = null;
   private JLabel highScore, score;
   private Scrollbar scrollbar;
   
   public Snake()
   {
      setLayout(new BorderLayout());
      
      board = new MyButton[SIZE][SIZE];
      
      JPanel center = new JPanel();
      center.setLayout(new GridLayout(SIZE, SIZE));
      
      JPanel top = new JPanel();
      top.setLayout(new GridLayout(1, 2));
      
      Listener listener = new Listener();
      
      highScore = new JLabel("High Score: 2");
      highScore.setHorizontalAlignment(JLabel.CENTER);
      top.add(highScore);
      
      score = new JLabel("Score: 2");
      score.setHorizontalAlignment(JLabel.CENTER);
      top.add(score);
      
      scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, 4, 1, 1, 32); //scrollbar from 1 to 8 with initial value of 1, determines speed of AI
      scrollbar.addAdjustmentListener(
         new AdjustmentListener(){
            public void adjustmentValueChanged(AdjustmentEvent e){
               aiSpeed = 4000 / SIZE / e.getValue();
            }});
      scrollbar.addKeyListener(listener); //allows for user to move the snake or toggle AI once he has used the scrollbar
      scrollbar.setBackground(Color.BLACK);
      
      for(int r = 0; r < SIZE; r++)
      {
         for(int c = 0; c < SIZE; c++)
         {
            board[r][c] = new MyButton(r, c);
            board[r][c].setBackground(Color.BLACK);
            board[r][c].setBorderPainted(false);
            board[r][c].addKeyListener(listener);
            center.add(board[r][c]);
         }
      }
      add(center, BorderLayout.CENTER);
      add(top, BorderLayout.NORTH);
      add(scrollbar, BorderLayout.SOUTH);
      startGame();
   }
   
   private void generateFood() //creates a new block of food in a random place on the board
   {
      int r = (int)(Math.random() * SIZE); //choose random row and random column
      int c = (int)(Math.random() * SIZE);
      while(board[r][c].getBackground() != Color.BLACK) //choose a new row and column until you find a black (empty) square to put it
      {
         r = (int)(Math.random() * SIZE);
         c = (int)(Math.random() * SIZE);
      }
      food = board[r][c];
      System.out.println("New food: " + food.getRow() + ", " + food.getCol());
      food.setBackground(Color.GREEN); //food is green
   }
   
   private void goUp()
   {
      if(dr != 1 || ai)
      {
         dr = -1;
         dc = 0;
      }
   }
   private void goDown()
   {
      if(dr != -1 || ai)
      {
         dr = 1;
         dc = 0;
      }
   }
   private void goRight()
   {
      if(dc != -1 || ai)
      {
         dr = 0;
         dc = 1;
      }
   }
   private void goLeft()
   {
      if(dc != 1 || ai)
      {
         dr = 0;
         dc = -1;
      }
   }
   
   private void startGame() //create the snake and the first food block
   {
      snake = new LinkedList<MyButton>();
      int r = (int)(Math.random() * (SIZE - 6)) + 3, c = (int)(Math.random() * (SIZE - 6)) + 3;
      snake.add(board[r][c]);
      snake.add(board[r + 1 < SIZE ? r + 1 : r - 1][c]);
      for(MyButton b : snake)
         b.setBackground(Color.RED);
      food = board[r][c + 1 < SIZE ? c + 1 : c - 1]; //put the food right next to the snake
      food.setBackground(Color.GREEN);
   }
   
   private void playGame() //start the mover thread
   {
      mover = new Thread(new SnakeMover());
      mover.start();
   }
   
   private void endGame() //reset variables, reset the board, kill the snake, reset labels
   {
      food.setBackground(Color.BLACK);
      food = null;
      for(MyButton b : snake)
         b.setBackground(Color.BLACK);
      mover = null;
      ai = false;
      dr = 0;
      dc = 0;
      score.setText("Score: 2");
      checked = null;
   }
   
   private class Listener implements KeyListener
   {
      public void keyPressed(KeyEvent e)
      {
         switch(e.getKeyCode()) //analyze the keyCode of the pressed key
         {
            case KeyEvent.VK_UP: 
               if(dr == 1)
                  break;
               goUp();
               break;
            case KeyEvent.VK_DOWN: 
               if(mover == null)
                  return;
               if(dr == -1)
                  break;
               goDown();
               break;
            case KeyEvent.VK_LEFT:
               if(dc == 1)
                  break;
               goLeft();
               break;
            case KeyEvent.VK_RIGHT:
               if(dc == -1)
                  break;
               goRight();
               break;
            case KeyEvent.VK_A: //toggle AI
               ai = !ai;
               break;
         }
         if(mover == null) //if the game hasn't started (snake is still), start it
            playGame();
         try //pause before accepting a new input from the keyboard
         {
            Thread.sleep(SPEED);
         }
         catch(Exception exc){}
      }
      public void keyTyped(KeyEvent e){}
      public void keyReleased(KeyEvent e){}
   }
   
   private class SnakeMover implements Runnable
   {
      public void run()
      {
         while(true) //do this forever (until death)
         {
            if(ai) //let the AI decide where to go if it's turned on
            {
               int r = snake.getFirst().getRow(), c = snake.getFirst().getCol();
               
               
               if(food.getRow() > r) //first try to reach the row of the food, then reach for the food's column
                  goDown(); 
               else if(food.getRow() < r)
                  goUp();
               else if(food.getCol() > c)
                  goRight();
               else if(food.getCol() < c)
                  goLeft();  
               
               /*
               int m = 0;
               if(Math.abs(food.getRow() - r) > m) {
            	   m = Math.abs(food.getRow() - r);
            	   if(food.getRow() > r)
                       goDown(); 
                   else
                       goUp();
               }
               if(Math.abs(food.getCol() - c) > m){
            	   if(food.getCol() > c)
                	   goRight();
                   else
                       goLeft();
               }*/
               
               if(neighbors(r, c, Color.RED) >= 2 || r + dr >= SIZE || c + dc >= SIZE || r + dr < 0 || c  + dc < 0 || board[r + dr][c + dc].getBackground() == Color.RED || neighbors(r + dr, c + dc, Color.RED) >= 2) //if the snake is going to die given its position and direction, change its direction
               //choose the direction that has the most empty space, and go there
               {
                  int areaMax = safeArea(r, c, dr, dc);
                  int right = safeArea(r, c, 0, 1);
                  int left = safeArea(r, c, 0, -1);
                  int up = safeArea(r, c, -1, 0);
                  int down = safeArea(r, c, 1, 0);
                  if(right > areaMax)
                  {
                     goRight();
                     areaMax = right;
                  }
                  if(left > areaMax)
                  {
                     goLeft();
                     areaMax = left;
                  }
                  if(up > areaMax)
                  {
                     goUp();
                     areaMax = up;
                  }
                  if(down > areaMax)
                  {
                     goDown();
                     //areaMax = down;
                  }
               }
            }
            //at this point, the ai has made a decision (if it's activated), so we must move the snake now
            int newR = snake.getFirst().getRow() + dr, newC = snake.getFirst().getCol() + dc;
            
            if(newR >= SIZE || newC >= SIZE || newR < 0 || newC < 0) //if the new row and new column are off the map, then you have run into a wall and lost
            {
            	
               System.out.println("death by running into wall, dr =  " + dr + ", dc = " + dc);
               endGame();
               startGame();
               return;
            	
            	//if you want to be able to go  through walls, enable  this
            	/*
            	if(newR >= SIZE)
            		newR = 0;
            	else if(newR < 0)
            		newR = SIZE - 1;
            	else if(newC >= SIZE)
            		newC = 0;
            	else if(newC < 0)
            		newC = SIZE - 1;
            	snake.getLast().setBackground(Color.BLACK); //make the tail black
                snake.getLast().updateUI();
                snake.removeLast(); //remove the tail
                board[newR][newC].setBackground(Color.RED); //set the new block to red
                board[newR][newC].updateUI();
                snake.addFirst(board[newR][newC]); //add the new block to the head of the snake
                */
            }
            else if(board[newR][newC].getBackground() == Color.RED) //if you run into yourself (any red square), you lose
            {
               System.out.println("death by running into self, dr = " + dr + ", dc = " + dc);
               endGame();
               startGame();
               return;
            }
            else if(board[newR][newC] == food) //if you run into food, grow the snake by 1
            {
               System.out.println("Food collected");
               board[newR][newC].setBackground(Color.RED); //set the food to red
               board[newR][newC].updateUI();
               snake.addFirst(board[newR][newC]); //add the food at the head of the snake
               generateFood(); //create a new food block
               score.setText("Score: " + snake.size());
               if(snake.size() > Integer.parseInt(highScore.getText().substring(12)))
                  highScore.setText("High Score: " + snake.size());
            }
            else //you have moved to a normal square, nothing special
            {
               snake.getLast().setBackground(Color.BLACK); //make the tail black
               snake.getLast().updateUI();
               snake.removeLast(); //remove the tail
               board[newR][newC].setBackground(Color.RED); //set the new block to red
               board[newR][newC].updateUI();
               snake.addFirst(board[newR][newC]); //add the new block to the head of the snake
            }
            try //sleep a different amount of time based on whether the AI is active or not
            {
               if(!ai)
                  Thread.sleep(SPEED);
               else
                  Thread.sleep(aiSpeed);
            }
            catch(Exception e){}
         }
         
      }
      
      private int safeArea(int r, int c, int dr, int dc) //calculate how much area (up to twice the length of the snake) that is available (empty) given the current position and a direction
      {
         checked = new LinkedList<MyButton>(); //reset the checked LinkedList
         int num = safeAreaHelper(r + dr, c + dc);
         //System.out.println("Running check: (" + r + ", " + c + ") dr = " + dr + ", dc = " + dc + ", safe area: " + num);
         return num;
      }
      
      private int safeAreaHelper(int r, int c) //help with safeArea
      {
         if(checked.size() >= /*snake.size() * 2.5*/ SIZE * SIZE - snake.size() - 1) //if the empty space is at least twice the length of the snake, return 0 to break from recursion
            return 0;
         boolean valid = r >= 0 && r < SIZE && c >= 0 && c < SIZE && !checked.contains(board[r][c]); //check if the current row and column are in bounds and haven't been checked yet
         if(valid && board[r][c].getBackground() != Color.RED) //if it is a valid square and it's not red, add it to checked and then check  the four surrounding squares
         {
            checked.addFirst(board[r][c]);
            return 1 + safeAreaHelper(r + 1, c) + safeAreaHelper(r - 1, c) + safeAreaHelper(r, c + 1) + safeAreaHelper(r, c - 1);
         }
         return 0; //if it is not valid, or it is valid but not black, return 0
      }
      
      public int neighbors(int r, int c, Color color)
      {
         int num = 0;
         if(r + 1 < SIZE && board[r + 1][c].getBackground() == color)
            num++;
         if(c + 1 < SIZE && board[r][c + 1].getBackground() == color)
            num++;
         if(r - 1 >= 0 && board[r - 1][c].getBackground() == color)
            num++;
         if(c - 1 >= 0 && board[r][c - 1].getBackground() == color)
            num++;
         return num;
      }
   }
   
   private class MyButton extends JButton //each button contains a row variable and a col variable, representing the row and the column of the button on the board
   {
      private int row, col;
      
      public MyButton(int r, int c)
      {
         row = r;
         col = c;
      }
      
      public int getRow()
      {
         return row;
      }
      
      public int getCol()
      {
         return col;
      }
   }
   
   public static void main(String[] args)
   {
      JFrame frame = new JFrame("Snake! by Joel Anglister");
      frame.setSize(750, 750);
      frame.setLocation(200, 20);      
      frame.setContentPane(new Snake());
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
   }
}