// SPACE GEMS 4K
// Made by Gavin Murrison, Feb 2014
// In tribute to the excellent SpaceChem!

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
//import java.util.Random;

//import java.awt.Event; // TODO: for keyboard access

public class G extends Applet implements Runnable {

  private int mouseX;
  private int mouseY;
  private boolean leftMouseDown;
  //private boolean rightMouseDown;
  //static boolean keys[] = new boolean[65536];

  @Override
  public void start() {
    enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
            /* | AWTEvent.KEY_EVENT_MASK */ ); // TODO: for keyboard access
    new Thread(this).start();
  }

  @Override
  @SuppressWarnings("fallthrough")
  public void run() {

    // CONSTANTS
    final boolean debug = false;
    final int viewWidth = 800;
    final int viewHeight = 600;
    final int gridCols = 14;
    final int gridRows = 10; // need to have at least 9 rows to fit in the toolbar
    final int cellWidth = viewWidth / gridCols;
    final int cellHeight = (viewHeight - 20) / gridRows;
    final int zoneSize = 4; // width and height of the input and output zones
    final int rightZoneStartCol = zoneSize + 2;
    final int targetGemsZoneStartCol = rightZoneStartCol + zoneSize;
    final int targetGemsZoneStartRow = 1;
    final int warningSize = viewHeight / 8;

    final int tickSleepTimeInMs = 10; // milliseconds to sleep after every game tick.
    // 10ms sleep time = 100 ticks per second
    final long nanosecondsPerSecond = 1000000000;
    final long initialStepNanoTime = nanosecondsPerSecond / 4;

    final int toolbarRow = 0; // TODO: move toolbar to bottom to avoid offset math?
    final int numTools = 13;
    final int targetGemClustersOutput = 3; // TODO: make this higher?
    final int maxGemTypes = 4; // A, B, C, D

    // Indices into the toolbar
    final int waldoStartToolIndex = 0;
    final char waldoStartSymbol = 'W';
    final int eraseToolIndex = 1;
    final char eraseSymbol = ' ';
    final char upSymbol = 'U';
    final char downSymbol = 'D';
    final char leftSymbol = 'L';
    final char rightSymbol = 'R';
    final char takeSymbol = 'T';
    final char putSymbol = 'P';
    final char input1Symbol = '1';
    final char input2Symbol = '2';
    final char outputSymbol = 'O';
    final char addBondSymbol = '+';
    final char removeBondSymbol = '-';
    //final char speedSymbol = 'S';
    
    // input and expected outputs for each level
    final String levelGems[][] =
    {
      // level A - carry correct gem to output zone
      {
        // input 1
        "    ",
        "    ",
        " A  ",
        "    ",

        // input 2
        "    ",
        "  B ",
        "    ",
        "    ",

        // output
        "    ",
        "  A ",
        "    ",
        "    ",
      },

      // level B - bond two gems together at an awkward angle
      {
        // input 1
        "    ",
        "    ",
        "  A ",
        "    ",

        // input 2
        "    ",
        " B  ",
        "    ",
        "    ",

        // output
        "  B ",
        "  A ",
        "    ",
        "    ",
      },

      // level C - bond single input gems into a 3-gem cluster, no corner issues
      {
        // input 1
        "    ",
        "    ",
        " C  ",
        "    ",

        // input 2
        "    ",
        "    ",
        "  D ",
        "    ",

        // output
        "    ",
        "    ",
        "  D ",
        " CD ",
      },

      // level D - bond single input gems into a 3-gem cluster, with drop-and-pickup
      {
        // input 1
        "    ",
        "    ",
        " C  ",
        "    ",

        // input 2
        "    ",
        "    ",
        "D   ",
        "    ",

        // output
        "    ",
        "    ",
        "  C ",
        "  DD",
      },
      
      // level E - complex gem clusters 1
      {
        // input 1
        "    ",
        "BC A",
        " ACD",
        "    ",

        // input 2
        "    ",
        " DBC",
        "BC A",
        "    ",

        // output
        "BC A",
        " ACD",
        " DBC",
        "BC A",
      },

      // level F - difficult manipulation
      {
        // input 1
        "    ",
        "BC A",
        " A D",
        "    ",

        // input 2
        "    ",
        " DBC",
        "BC A",
        "    ",

        // output
        "    ",
        "BC A",
        " A D",
        " DBC",
      },
      
      // level G - interlocking complex gem clusters
      {
        // input 1
        "    ",
        " DBC",
        " C A",
        " A D",

        // input 2
        "    ",
        " C A",
        " A D",
        " DBC",

        // output
        " DBC",
        "CCAA",
        "AADD",
        "DBC ",
      },
    };

    // VARIABLES
    // TODO: replace local scratch variables with fewer global scratch variables?

    // Additional game state.
    int level = 0;
    int numGemClustersOutput = 0;
    int waldoStartCol = 8;
    int waldoStartRow = 5;
    int waldoCol;
    int waldoRow;
    int waldoColDir = -1;
    int waldoRowDir = 0;
    char waldoGemGroup = 0;
    char highestGemGroup = waldoGemGroup;
    int selectedTool = 0;
    long levelResetTime = 0; // nanosecond time at which to reset level

    // Misc variables.
    long stepNanoTime = initialStepNanoTime;
    int frames = 0;
    int fps = 0;
    int steps = 0;
    long lastFpsTime = 0;
    long lastStepTime = System.nanoTime();
    int warnCol = 0;
    int warnRow = 0;
    boolean hasWarning = false;

    // Operations on the grid
    char operatorGrid[][] = new char[gridCols][gridRows];

    // Create a toolbar of operations within the grid
    operatorGrid[waldoStartToolIndex][toolbarRow] = waldoStartSymbol;
    operatorGrid[eraseToolIndex][toolbarRow] = eraseSymbol;
    operatorGrid[2][toolbarRow] = upSymbol;
    operatorGrid[3][toolbarRow] = downSymbol;
    operatorGrid[4][toolbarRow] = leftSymbol;
    operatorGrid[5][toolbarRow] = rightSymbol;
    operatorGrid[6][toolbarRow] = takeSymbol;
    operatorGrid[7][toolbarRow] = putSymbol;
    operatorGrid[8][toolbarRow] = input1Symbol;
    operatorGrid[9][toolbarRow] = input2Symbol;
    operatorGrid[10][toolbarRow] = outputSymbol;
    operatorGrid[11][toolbarRow] = addBondSymbol;
    operatorGrid[12][toolbarRow] = removeBondSymbol;

    if(debug)
    {
      // TODO: level setup for debugging
      waldoStartCol = 5;
      waldoStartRow = 7;
      operatorGrid[3][7] = input2Symbol;
      operatorGrid[2][7] = input1Symbol;
      operatorGrid[1][7] = upSymbol;
      operatorGrid[1][6] = takeSymbol;
      operatorGrid[1][4] = addBondSymbol;
      operatorGrid[1][3] = putSymbol;
      operatorGrid[1][2] = rightSymbol;
      operatorGrid[2][2] = takeSymbol;
      //operatorGrid[3][1] = removeBondSymbol;
      operatorGrid[8][2] = putSymbol;
      operatorGrid[9][2] = downSymbol;
      operatorGrid[9][4] = outputSymbol;
      operatorGrid[9][7] = leftSymbol;
    }
    
    // Place a waldo start operator somewhere
    operatorGrid[waldoStartCol][waldoStartRow] = waldoStartSymbol;
    
    // Make waldo start from a waldo start operator
    waldoCol = waldoStartCol;
    waldoRow = waldoStartRow;

    // Gems on the grid (double-buffered for easier movement)
    char gemTypeGrid[][] = new char[gridCols][gridRows];
    char gemGroupGrid[][] = new char[gridCols][gridRows];
    char gemTypeGridNew[][] = new char[gridCols][gridRows];
    char gemGroupGridNew[][] = new char[gridCols][gridRows];

    // Create some initial gems
/*
    highestGemGroup++;
    gemTypeGrid[waldoCol][waldoRow] = 'A';
    gemGroupGrid[waldoCol][waldoRow] = highestGemGroup;
    gemTypeGrid[waldoCol + 1][waldoRow] = 'B';
    gemGroupGrid[waldoCol][waldoRow] = highestGemGroup;
    gemTypeGrid[waldoCol][waldoRow + 1] = 'C';
    gemGroupGrid[waldoCol][waldoRow + 1] = highestGemGroup;
    waldoGemGroup++;
*/
    
    // TODO: for AppletViewer, remove later.
    setSize(viewWidth, viewHeight);

    // Set up the graphics stuff, double-buffering.
    BufferedImage screen = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_RGB);
    Graphics g = screen.getGraphics();
    Graphics appletGraphics = getGraphics();

    // Set font to use with all displayed text
    g.setFont(Font.decode("Dialog-BOLD-15"));
//    g.setFont(Font.decode("Arial-BOLD-15"));

    // Game loop - one tick per iteration
    while(true)
    //for(int ticks = 0; ; ticks++)
    {
      //int ticks = (int)(System.nanoTime() / 1000000000L);

      // TODO: use System.currentTimeMillis instead?
      long currTime = System.nanoTime();

      // Reset the game state?
      if(currTime > levelResetTime)
      {
        levelResetTime = initialStepNanoTime * initialStepNanoTime;
        stepNanoTime = initialStepNanoTime;
        lastStepTime = currTime;

        // reset the warning
        warnCol = 0;
        warnRow = 0;
        hasWarning = false;

        // reset the waldo to move from the waldo start
        waldoCol = waldoStartCol;
        waldoRow = waldoStartRow;

        // reset the waldo to move to the left
        waldoColDir = -1;
        waldoRowDir = 0;

        // reset the gem grids
        gemTypeGrid = new char[gridCols][gridRows];
        gemGroupGrid = new char[gridCols][gridRows];
        gemTypeGridNew = new char[gridCols][gridRows];
        gemGroupGridNew = new char[gridCols][gridRows];
      }

      if(currTime - lastStepTime > stepNanoTime)
      {
        // Move the Waldo one step, keeping it within bounds
        steps++;
        lastStepTime = currTime;
        waldoCol += waldoColDir;
        waldoRow += waldoRowDir;
        //waldoCol = Math.max(0, Math.min(waldoCol + waldoColDir, gridCols - 1));
        //waldoRow = Math.max(1, Math.min(waldoRow + waldoRowDir, gridRows - 1));

        // warn if the waldo will collide with the grid boundary
        // TODO: row bounds have to change if toolbar is moved!
        if(waldoCol < 0 || waldoCol >= gridCols ||
           waldoRow < 1 || waldoRow >= gridRows)
        {
          waldoCol -= waldoColDir;
          waldoRow -= waldoRowDir;

          warnCol = waldoCol;
          warnRow = waldoRow;
        }

        // Move any gems that the Waldo is holding, alongside the Waldo
        // Because of double-buffering of gem grids, even stationery gems
        // need to be copied to the other set of grids!
        for(int col = 0; col < gridCols; col++)
        {
          for(int row = 0; row < gridRows; row++)
          {
            int newCol = col;
            int newRow = row;
            char gemGroup = gemGroupGrid[col][row];
            if(gemGroup == waldoGemGroup)
            {
              // gem is attached to waldo, so move gem with waldo
              newCol = Math.max(0, Math.min(col + waldoColDir, gridCols - 1));
              newRow = Math.max(1, Math.min(row + waldoRowDir, gridRows - 1));
              // TODO: row bounds above have to change if toolbar is moved!
            }

            // only move actual gems, not empty space (which might overwrite gems!)
            if(gemGroup != 0)
            {
              // warn if this gem will collide with another gem
              if(gemTypeGridNew[newCol][newRow] != 0)
              {
                warnCol = newCol;
                warnRow = newRow;
              }

              // gem moved without colliding with a boundary
              gemGroupGridNew[newCol][newRow] = gemGroupGrid[col][row];
              gemGroupGrid[col][row] = 0;
              gemTypeGridNew[newCol][newRow] = gemTypeGrid[col][row];
              gemTypeGrid[col][row] = 0;
            }
          }
        }

        // swap old and new gem grids
        char[][] tempGrid = gemTypeGrid;
        gemTypeGrid = gemTypeGridNew;
        gemTypeGridNew = tempGrid;

        tempGrid = gemGroupGrid;
        gemGroupGrid = gemGroupGridNew;
        gemGroupGridNew = tempGrid;

        // perform operation based on current grid cell under Waldo
        char operation = operatorGrid[waldoCol][waldoRow];
        int zoneStartRow = -1;
        switch(operation)
        {
          case downSymbol:
            waldoColDir = 0;
            waldoRowDir = +1;
            break;

          case upSymbol:
            waldoColDir = 0;
            waldoRowDir = -1;
            break;

          case leftSymbol:
            waldoColDir = -1;
            waldoRowDir = 0;
            break;

          case rightSymbol:
            waldoColDir = +1;
            waldoRowDir = 0;
            break;
            
          case putSymbol:
            waldoGemGroup = 0;
            break;

          case takeSymbol:
            // TODO: Waldo should not be able to pick up a gem cluster unless
            // it is over one of the gems in the cluster!
            waldoGemGroup = gemGroupGrid[waldoCol][waldoRow];
            break;

          // handle both input symbols with the same code block
          case input1Symbol:
            zoneStartRow = 1;
            // intentional fall-through to next case statement
          case input2Symbol:
            if(zoneStartRow == -1)
              zoneStartRow = 1 + zoneSize;

            // place a cluster of gems in the input zone
            highestGemGroup++;
            for(int zoneCol = 0; zoneCol < zoneSize; zoneCol++)
            {
              for(int zoneRow = 0; zoneRow < zoneSize; zoneRow++)
              {
                char type = levelGems[level][zoneStartRow - 1 + zoneRow].charAt(zoneCol);
                if(type != ' ')
                {
                  int row = zoneStartRow + zoneRow;

                  // warn if this gem will collide with another gem
                  if(gemTypeGrid[zoneCol][row] != 0)
                  {
                    warnCol = zoneSize / 2;
                    warnRow = zoneStartRow + zoneSize / 2;
                  }
                  
                  gemTypeGrid[zoneCol][row] = type;
                  gemGroupGrid[zoneCol][row] = highestGemGroup;
                  // TODO: row math above has to change if toolbar is moved!
                }
              }
            }
            break;

          // TODO: merge output logic with input logic to reduce code size? Could be tricky!
          case outputSymbol:
            // remove a cluster of gems from the output zone,
            // and verify that they match what is expected
            // TODO: any gems outside the output zone are ignored,
            // even if they are attached to gems in the output zone!
            boolean matches = true;
            for(int zoneCol = 0; zoneCol < zoneSize; zoneCol++)
            {
              for(int zoneRow = 0; zoneRow < zoneSize; zoneRow++)
              {
                // which type of gem do we expect at this grid cell?
                char type = levelGems[level][zoneSize * 2 + zoneRow].charAt(zoneCol);
                if(type == ' ')
                  type = 0;
                
                // determine (global) coordinates of grid cell
                int col = rightZoneStartCol + zoneCol;
                int row = zoneRow + 1;
                // TODO: row math above has to change if toolbar is moved!

                // is gem not of expected type?
                if(gemTypeGrid[col][row] != type)
                {
                  matches = false;
                  warnCol = rightZoneStartCol + zoneSize / 2;
                  warnRow = zoneSize / 2 + 1;
                }

                // erase gem as it is being output
                gemTypeGrid[col][row] = 0;
                gemGroupGrid[col][row] = 0;
              }
            }

            if(matches)
              numGemClustersOutput++;
            if(numGemClustersOutput == targetGemClustersOutput)
            {
              // Move to next level, wrapping around from last to first levels
              level = (level + 1) % levelGems.length;
              numGemClustersOutput = 0;
            }
            break;

          case addBondSymbol:
            char targetGroup = gemGroupGrid[waldoCol][waldoRow];

            for(int dir = 0; dir < 4; dir++)
            {
              final int[] dirOffset = {0, 0, -1, +1};
              final int colOffset = dirOffset[dir];
              final int rowOffset = dirOffset[(dir + 2) % 4];

              // TODO: check for grid out of bounds. Bonding at boundary hangs game!
              char sourceGroup = gemGroupGrid[waldoCol + colOffset][waldoRow + rowOffset];
              if(targetGroup != 0 && sourceGroup != 0 && sourceGroup != targetGroup)
              {
                // change all gems in other group to curr group
                for(int col = 0; col < gridCols; col++)
                {
                  for(int row = 0; row < gridRows; row++)
                  {
                    if(gemGroupGrid[col][row] == sourceGroup)
                      gemGroupGrid[col][row] = targetGroup;
                  }
                }
              }
            }
            break;

          case removeBondSymbol:
            // Unbond the gem under the Waldo from any cluster of gems.
            // If Waldo was holding the gem, it continues holding the gem.
            highestGemGroup++;
            if(waldoGemGroup == gemGroupGrid[waldoCol][waldoRow])
              waldoGemGroup = highestGemGroup;
            gemGroupGrid[waldoCol][waldoRow] = highestGemGroup;
            break;

/*
          case speedSymbol:
            if(stepNanoTime == initialStepNanoTime)
              stepNanoTime = initialStepNanoTime / 10;
            else
              stepNanoTime = initialStepNanoTime;
            break;
 */
        }
      }

      // Create or select an operation?
      if(leftMouseDown)
      //if(leftMouseDown && mouseX < viewWidth && mouseY < viewHeight) // TODO: did not work
      {
        int col = mouseX / cellWidth;
        int row = mouseY / cellHeight;
        
        if(col >= gridCols || row >= gridRows)
        {
          // do nothing - click was outside of grid
        }
        else if(row == toolbarRow)
        {
          // selecting a tool from the toolbar?
          if (col < numTools)
            selectedTool = col;
        }
        else
        {
          // did we just place an erase tool?
          if(selectedTool == eraseToolIndex)
            // erase the operation on the grid
            operatorGrid[col][row] = 0;
          else
            // place a tool onto the grid
            operatorGrid[col][row] = operatorGrid[selectedTool][toolbarRow];

          // did we just place a waldo start?
          if(selectedTool == waldoStartToolIndex)
          {
            // remove the previous waldo start operator
            operatorGrid[waldoStartCol][waldoStartRow] = 0;
            
            // remember where we placed the waldo start
            waldoStartCol = col;
            waldoStartRow = row;

            // reset the waldo to move from the waldo start
            waldoCol = waldoStartCol;
            waldoRow = waldoStartRow;

            // reset the waldo to move to the left
            waldoColDir = -1;
            waldoRowDir = 0;
          }
        }
        
        leftMouseDown = false;
      }

      // Draw background
      g.setColor(Color.black);
      g.fillRect(0, 0, viewWidth, viewHeight);

      // Draw input and output zones, and target gem zone
      g.setColor(new Color(0x400040));
      //g.setColor(Color.magenta.darker().darker());
      g.fillRect(0, cellHeight, zoneSize * cellWidth, zoneSize * cellHeight);
      g.setColor(new Color(0x004040));
      g.fillRect(0, (zoneSize + 1) * cellHeight, zoneSize * cellWidth, zoneSize * cellHeight);
      g.setColor(new Color(0x404000));
      g.fillRect(rightZoneStartCol * cellWidth, cellHeight, zoneSize * cellWidth, zoneSize * cellHeight);
//      g.setColor(new Color(0x404040));
//      g.fillRect(targetGemsZoneStartCol * cellWidth, cellHeight, zoneSize * cellWidth, zoneSize * cellHeight);

      // Draw a grid of cell outlines and operations
      for(int row = 0; row < gridRows; row++)
      {
        int numCols = targetGemsZoneStartCol;
        if(row == toolbarRow)
          numCols = numTools;
        
        for(int col = 0; col < numCols; col++)
        {
          g.setColor(Color.gray);
          g.drawRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);

          if(operatorGrid[col][row] != 0)
          {
            if(row == toolbarRow)
              if(col == selectedTool)
                g.setColor(Color.yellow);
              else
                g.setColor(Color.green);
            else
              g.setColor(Color.red);

            g.fillRect(col * cellWidth + 1, row * cellHeight + 1, cellWidth - 1, cellHeight - 1);
            g.setColor(Color.black);
            String text = String.valueOf(operatorGrid[col][row]);
            g.drawString(text, col * cellWidth + 5, row * cellHeight + 20);
          }
        }
      }

      // Draw the Waldo
      g.setColor(Color.blue);
//      g.fillRect(waldoCol * cellWidth, waldoRow * cellHeight, cellWidth / 2, cellHeight / 2);
      g.fillOval(waldoCol * cellWidth, waldoRow * cellHeight, cellWidth, cellHeight);
      
      // Draw the grid of gems
      for(int row = 0; row < gridRows; row++)
      {
        for(int col = 0; col < gridCols; col++)
        {
          int gemType = gemTypeGrid[col][row];
          
          int targetCol = col - targetGemsZoneStartCol;
          int targetRow = row - targetGemsZoneStartRow;
          if(targetCol >= 0 && targetRow >= 0 && targetRow < zoneSize)
          {
            // ignore gem grid - instead display the target gem cluster that needs to be matched
            gemType = levelGems[level][zoneSize * 2 + targetRow].charAt(targetCol);
            if(gemType == ' ')
              gemType = 0;
          }
          
          if(gemType != 0)
          {
            // color gem based on gem type
            float hue = (float)(gemType - 'A' + 0.5) / maxGemTypes; // avoid making gems red!
            g.setColor(new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f)));
            //g.setColor(Color.pink);
            
            // TODO: decide on final gem appearance
            // TODO: draw as diamond shape so that gems touch together without hiding operator chars?
            g.fillRoundRect(col * cellWidth + cellWidth / 4, row * cellHeight + cellHeight / 4, cellWidth / 2, cellHeight / 2, cellWidth / 4, cellHeight / 4);
            //g.fill3DRect(col * cellWidth + cellWidth / 4, row * cellHeight + cellHeight / 4, cellWidth / 2, cellHeight / 2, false);
            //g.fillRect(col * cellWidth + cellWidth / 2, row * cellHeight + cellHeight / 2, cellWidth / 2, cellHeight / 2);
            
/*            
            if(debug)
            {
              // draw gem group number, for debugging
              g.setColor(Color.white);
              String text = String.valueOf(gemType) + String.valueOf((int)gemGroupGrid[col][row]);
              g.drawString(text, (int)((col + 0.3) * cellWidth), (int)((row + 0.3) * cellHeight + 10));
            }
*/            
          }
        }
      }

      // Optionally draw large warning symbol
      if(warnCol != 0 || warnRow != 0)
      {
        if(!hasWarning)
        {
          hasWarning = true;
          
          // pause simulation and reset level in a few seconds time
          levelResetTime = currTime + nanosecondsPerSecond * 3;
          stepNanoTime = initialStepNanoTime * 1000;
        }

        g.setColor(Color.yellow);
        int centreX = warnCol * cellWidth + cellWidth / 2;
        int centreY = warnRow * cellHeight + cellHeight / 2;
        g.drawLine(centreX - warningSize, centreY - warningSize, centreX + warningSize, centreY + warningSize);
        g.drawLine(centreX - warningSize, centreY + warningSize, centreX + warningSize, centreY - warningSize);
      }

      // Draw text
      g.setColor(Color.white);
      g.drawString("Match the pattern above",
              targetGemsZoneStartCol * cellWidth + cellWidth / 2, (targetGemsZoneStartRow + zoneSize) * cellHeight + 20);
      g.drawString("Level " + (level+1) + ", Collected " + numGemClustersOutput + "/" + targetGemClustersOutput,
              targetGemsZoneStartCol * cellWidth + cellWidth / 2, (targetGemsZoneStartRow + zoneSize + 3) * cellHeight);
      
      if(debug)
      {
        // Draw debugging text
        g.drawString("Level " + (level+1) + ", Selected Tool " + selectedTool + ", Step " + steps + ", FPS " + String.valueOf(fps) +
                ", Waldo Pos " + waldoCol + ":" + waldoRow + ", Waldo Gem Group " + (int)waldoGemGroup +
                ", Clusters Output " + numGemClustersOutput + "/" + targetGemClustersOutput + ", StepNanoTime " + stepNanoTime,
                50, viewHeight - 10);
      }

      // Copy the entire rendered image onto the screen
      appletGraphics.drawImage(screen, 0, 0, null);

      // Lock frame rate
      try
      {
        Thread.sleep(tickSleepTimeInMs);
      }
      catch (Exception e)
      {
      };

      // Measure frame rate
      frames++;
      if(frames >= 10)
      {
        long delta = System.nanoTime() - lastFpsTime;
        fps = (int)(100000000L * frames / delta);
        frames = 0;
        lastFpsTime = System.nanoTime();
      }

      // Should applet quit?
      if (!isActive()) {
        return;
      }
    }
  }

  @Override
  public void processMouseEvent(MouseEvent e)
  {
    mouseX = e.getX();
    mouseY = e.getY();
    //mouseDown = (e.getButton() == MouseEvent.BUTTON1);
    leftMouseDown = ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0);
    //rightMouseDown = ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0);
  }

/*
  // TODO: for keyboard access
  public boolean handleEvent(Event e) {
    //keys[e.getKeyCode()] = (e.getID() == KeyEvent.KEY_PRESSED);

    switch (e.id) {
          case Event.KEY_PRESS:
          case Event.KEY_ACTION:
              // key pressed
              break;
          case Event.KEY_RELEASE:
              // key released
              break;
          case Event.MOUSE_DOWN:
              // mouse button pressed
              //mouseDown = true;
              break;
          case Event.MOUSE_UP:
              // mouse button released
              //mouseDown = false;
              break;
          case Event.MOUSE_MOVE:
              //mouseX = (MouseEvent)e;
              break;
          case Event.MOUSE_DRAG:
              break;
     }
     return false;
  }
*/
}