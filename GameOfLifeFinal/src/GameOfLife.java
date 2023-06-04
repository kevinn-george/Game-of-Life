
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet.ColorAttribute;



public class GameOfLife {
	public static void main(String[] args) {
		EventQueue.invokeLater( new Runnable() {
			@Override public void run() {
				
				JFrame frame = new JFrame("Game Of Life");
				frame.setSize(1200,800);
				frame.setResizable(true);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	            frame.setLocationRelativeTo(null);
	            frame.setVisible(true);
                frame.getContentPane().setBackground(Color.BLACK);

	            GridModel model = new GridModel();
	            GridPanel grid = new GridPanel(model);
	            GameView panel = new GameView();
	            GameController controller = new GameController(grid, model, panel);
	            
	            new Thread(controller).start();
	            new Thread(panel).start();
	            
	            
	            frame.add(panel);
			}
		});
	}

}

class GameController implements Runnable {
	private GridModel model;
	private GridPanel grid;
	private GameView panel;
	AliveCell[] cells;
	private Preferences pref;

	private boolean start = false;
	private Timer Time;
	
	GameController(GridPanel grid, GridModel model, GameView panel) {
		this.grid = grid;
		this.model = model;
		this.panel = panel;

		pref = Preferences.userRoot().node(getClass().getName() + " Game");
		cells = new AliveCell[GridModel.GRIDSIZE];
		
        for (int i = 0; i < GridModel.GRIDSIZE; i++) 
        	cells[i] = new AliveCell(i, cells);
        
        model.setAllCells(cells);
		
		try {
			if(Preferences.userRoot().nodeExists(getClass().getName() + " Game") == false) {
				pref.putInt("speed", 1);
                pref.putInt("pattern", 0);
                pref.putInt("zoom", 1);
                pref.putBoolean("restore", false);
                pref.putByteArray("grid", getCellsByteArray());
			}
		}catch (BackingStoreException e) {
            e.printStackTrace();
        }
		
		panel.setPatterns(model.getPatterns());
		panel.setPrefPatterns(model.getPatterns());
		panel.setSpeeds(model.getSpeeds());
		panel.setPrefSpeeds(model.getSpeeds());
		panel.setSizes(model.getSizes());
		panel.setPrefSizes(model.getSizes());
		panel.createGridPanel(grid);
		
		Time = new Timer(model.getSpeed(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.increaseGeneration();
                updateGame();
        }});
		
		panel.addSubmitListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
                pref.putInt("speed", panel.getCurrentSpeed());
                pref.putInt("pattern", panel.getCurrentPattern());
                pref.putInt("zoom", panel.getCurrentSize());
                pref.putBoolean("save", panel.isSavePref());
                panel.setPrefsDialogVisible(false);
			}
		});
		
		panel.addNextListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.increaseGeneration();
				updateGame();
			}
		});
		
		panel.addStartListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!Time.isRunning()) {
					Time.start();
                    panel.setNextBoolean(false);
                    panel.setStartText("Stop");
                } 
				else {
                	Time.stop();
                    panel.setNextBoolean(true);
                    panel.setStartText("Start");
                }
			}
		});
		
		panel.addSizesListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Point gridOffset = grid.getGridOffset();
				
                if (panel.getCurrentSize() == 0) {
                	gridOffset = new Point();
                	updateSize();
                }
     
                else if (gridOffset.x >= (model.getGridSize() * model.getBlockSize()) / 1.5) {
                    gridOffset.setLocation((model.getGridSize() * model.getBlockSize()) / 2, gridOffset.y);
                }
                
                else if (gridOffset.x <= -(model.getGridSize() * model.getBlockSize()) / 1.5) {
                    gridOffset.setLocation(-(model.getGridSize() * model.getBlockSize()) / 2, gridOffset.y);
                }
                
                else if (gridOffset.y >= (model.getGridHeight() * model.getBlockSize()) / 1.5) {
                    gridOffset.setLocation(gridOffset.x, (model.getGridHeight() * model.getBlockSize()) / 2);
                }
                
                else if (gridOffset.y <= -(model.getGridHeight() * model.getBlockSize()) / 1.5) {
                    gridOffset.setLocation(gridOffset.x, -(model.getGridHeight() * model.getBlockSize()) / 2);
                }
                
                grid.setGridOffset(gridOffset);
                panel.repaint();
			}
		});
		
		panel.addSpeedsListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (panel.getCurrentSpeed()) {
                case 0: 
                	model.setSpeed(600); 
                	break;
                case 1: 
                	model.setSpeed(300); 
                	break;
                case 2: 
                	model.setSpeed(100); 
                	break;
            }

            Time.setDelay(model.getSpeed());
            if (Time.isRunning()) {
            	Time.restart();
            }
		}});
		
		panel.addPatternListListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Time.stop();
				panel.setNextBoolean(true);
                panel.setStartText("Start");
                model.setCurrentAge(0);
                
                AliveCell[] cells = model.getAllCells();
                
                for (AliveCell cell: cells) {
                	cell.setAlive(false);
                }
                
				int MiddleX = model.getGridSize() / 2;
				int	MiddleY = model.getGridHeight() / 2;
                int InitialGrid = MiddleX + MiddleY * model.getGridSize();
                
				switch (panel.getPatternListSelectedString()) {
				case "Blinker":
					cells[InitialGrid].setAlive(true);
					cells[InitialGrid + 1].setAlive(true);
					cells[InitialGrid + 2].setAlive(true);
					break;
					
				case "Block": 
                    cells[InitialGrid].setAlive(true);
                    cells[InitialGrid + 1].setAlive(true);
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    break;
                    
                case "Tub": 
                    cells[InitialGrid + 1].setAlive(true);
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    break;
                    
                case "Boat": 
                    cells[InitialGrid + 1].setAlive(true);
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                    break;
                    
                case "Glider" : 
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                    break;     
                    
                case "Ship": 
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid + 1].setAlive(true);
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                    break;
                                      
                case "Beehive" : 
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid + 2].setAlive(true);
                    cells[InitialGrid + model.getGridSize()].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                    break;
                    
                case "Barge" : 
                    cells[InitialGrid].setAlive(true);
                    cells[InitialGrid + 2].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() * 2].setAlive(true);
                    break;
                    
                case "Toad" : 
                	cells[InitialGrid].setAlive(true);
                	cells[InitialGrid - 1].setAlive(true);
                	cells[InitialGrid - 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize()].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize() - 1].setAlive(true);
                    break;
                    
                case "Beacon" : 
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize()].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 2].setAlive(true);
                    break;    
                    
                case "Long Boat" : 
                    cells[InitialGrid].setAlive(true);
                    cells[InitialGrid + 2].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() * 2 - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid - model.getGridSize() * 2].setAlive(true);
                    break;
                                        
                case "Loaf" : 
                    cells[InitialGrid - 1].setAlive(true);
                    cells[InitialGrid + 2].setAlive(true);
                    cells[InitialGrid + model.getGridSize() - 1].setAlive(true);
                    cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                    cells[InitialGrid + model.getGridSize() * 2].setAlive(true);
                    cells[InitialGrid - model.getGridSize()].setAlive(true);
                    cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                    break;
                    
                case "Pond" : 
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid - 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize()].setAlive(true);
                	cells[InitialGrid + model.getGridSize() - 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() + 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 1].setAlive(true);
                    break;
                    
                case "Mango":
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize()].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize() - 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 1].setAlive(true);
                	break;
                	
                case "Long Barge":
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid - 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize()].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize()].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 1].setAlive(true);
                	break;
                	
                case "Half-Fleet":
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid + 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 3].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 3].setAlive(true);
                	cells[InitialGrid - model.getGridSize()].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 3 - 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 3 - 2].setAlive(true);
                	break;
                	
                case "Half-Bakery":
                	cells[InitialGrid + 1].setAlive(true);
                	cells[InitialGrid - 1].setAlive(true);
                	cells[InitialGrid + 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize()].setAlive(true);
                	cells[InitialGrid - model.getGridSize() - 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 2 - 3].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 3 - 1].setAlive(true);
                	cells[InitialGrid - model.getGridSize() * 3 - 2].setAlive(true);
                	cells[InitialGrid + model.getGridSize()].setAlive(true);
                	cells[InitialGrid + model.getGridSize() + 3].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 1].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 2 + 3].setAlive(true);
                	cells[InitialGrid + model.getGridSize() * 3 + 2].setAlive(true);
                	break;
				}
			}});
	
	grid.addMouseListener(new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (panel.isEditable() == true && !Time.isRunning() && (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.NOBUTTON)) {
                int offsetX = (grid.getWidth() / 2) - ((model.getGridSize() * model.getBlockSize()) / 2) + grid.getGridOffset().x;
                int offsetY = (grid.getHeight() / 2) - ((model.getGridHeight() * model.getBlockSize()) / 2) + grid.getGridOffset().y;

                int selectedIndex = -1;
                
                for (int i = 0; i < model.getGridSize(); i++) {
                	
                    for (int j = 0; j < model.getGridHeight(); j++) {
                        int index = i + j * GridModel.GRIDWIDTH;
                        int posX = i * model.getBlockSize() + offsetX, posY = j * model.getBlockSize() + offsetY;

                        if (e.getPoint().getX() >= posX && e.getPoint().getX() <= posX + model.getBlockSize() - 3 && e.getPoint().getY() >= posY && e.getPoint().getY() <= posY + model.getBlockSize() - 3) {
                            selectedIndex = index;
                            break;
                        }
                    }

                    if (selectedIndex > -1) {
                    	break;
                    }
                }

                if (selectedIndex > -1) {
                    model.getCell(selectedIndex).setAlive(!model.getCell(selectedIndex).isAlive());
                }
                
            }
            
            else if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu rightClickMenu = new JPopupMenu();
                JMenuItem save = new JMenuItem("Save"), open = new JMenuItem("Open"), pref = new JMenuItem("Preferences");

                save.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        GridSetup gridConfig = new GridSetup(model.getAllCells(), model.getGeneration(), model.getSpeed(),
                                panel.getCurrentSize(), panel.getCurrentPattern(), panel.isEnabled(), grid.getGridOffset());

                        FileDialog fileDialog = new FileDialog(panel.getTopFrame(), "Save Grid Configuration", FileDialog.SAVE);

                        fileDialog.setFile("*.life");
                        fileDialog.setVisible(true);

                        if (fileDialog.getFile() == null) return;
                        if (!fileDialog.getFile().endsWith(".life"))
                            fileDialog.setFile(fileDialog.getFile() + ".life");

                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(fileDialog.getDirectory() + "/" + fileDialog.getFile());
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                            objectOutputStream.writeObject(gridConfig);
                            objectOutputStream.flush();
                            objectOutputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        
                    }});
                    
               
        open.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        FileDialog fileDialog = new FileDialog(panel.getTopFrame(), "Load Grid Configuration", FileDialog.LOAD);
                        fileDialog.setFile("*.life");
                        fileDialog.setVisible(true);

                        if (fileDialog.getFile() == null) return;
                        if (!fileDialog.getFile().endsWith(".life")) {
                            JOptionPane.showMessageDialog(panel.getTopFrame(), "Please select a file with the extension \".life\".");
                            return;
                        }

                        GridSetup gridConfig = null;
                        try {
                            FileInputStream fileInputStream = new FileInputStream(fileDialog.getDirectory() + "/" + fileDialog.getFile());
                            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                            gridConfig = (GridSetup) objectInputStream.readObject();
                            objectInputStream.close();
                        } catch (ClassNotFoundException | IOException e1) {
                            e1.printStackTrace();
                        }

                        if (gridConfig != null) {
                            panel.setPattern(gridConfig.getStartingPattern());
                            model.setAllCells(gridConfig.getCells());
                            model.setGeneration(gridConfig.getGeneration());
                            model.setGeneration(gridConfig.getSpeed());

                            int speedIndex = 0;
                            
                            if (model.getSpeed() == 300) {
                            	speedIndex = 1;
                            }
                            
                            else if (model.getSpeed() == 600) {
                            	speedIndex = 2;
                            }
                        

                            
                        panel.setSpeed(speedIndex);
                            panel.setSize(gridConfig.getSize());
                            panel.setEditable(gridConfig.getEditMode());
                            grid.setGridOffset(gridConfig.getGridOffset());
                        }
                    }});

                
        pref.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        panel.setEditable(GameController.this.pref.getBoolean("save", false)); 
                        panel.setPrefPattern(GameController.this.pref.getInt("pattern", 0));
                        panel.setPrefSpeed(GameController.this.pref.getInt("speed", 1));
                        panel.setPrefSize(GameController.this.pref.getInt("zoom", 0));
                        panel.setPrefsDialogVisible(true);
                    }});

                rightClickMenu.add(save);
                rightClickMenu.add(open);
                rightClickMenu.add(pref);

                rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
                grid.addMouseMotionListener(new MouseMotionListener() {
        			
                    public void mouseDragged(MouseEvent e) {
                        if (e.getButton() != MouseEvent.BUTTON1 && e.getButton() != MouseEvent.NOBUTTON) return;

                        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                        Point currentPoint = e.getPoint(),
                                currentOffset = new Point(currentPoint.x - grid.getMouseDragPoint().x, currentPoint.y - grid.getMouseDragPoint().y);

                        if (grid.getGridOffset().x >= (model.getGridSize() * model.getBlockSize()) / 2 && currentOffset.x > 0)
                            currentOffset.setLocation(0, currentOffset.y);
                        if (grid.getGridOffset().x <= -(model.getGridSize() * model.getBlockSize()) / 2 && currentOffset.x < 0)
                            currentOffset.setLocation(0, currentOffset.y);

                        if (grid.getGridOffset().y >= (model.getGridHeight() * model.getBlockSize()) / 2 && currentOffset.y > 0)
                            currentOffset.setLocation(currentOffset.x, 0);
                        if (grid.getGridOffset().y <= -(model.getGridHeight() * model.getBlockSize()) / 2 && currentOffset.y < 0)
                            currentOffset.setLocation(currentOffset.x, 0);

                        grid.setGridOffset(new Point(grid.getGridOffset().x + currentOffset.x, grid.getGridOffset().y + currentOffset.y));
                        grid.setMouseDragPoint(e.getPoint());
                    }

					@Override
					public void mouseMoved(MouseEvent e) {
						// TODO Auto-generated method stub
						
					}
                    
            	});
            }
        }

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub	
			 if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.NOBUTTON)
                 grid.setMouseDragPoint(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub		
		      if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.NOBUTTON)
                  panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub	
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub			
		}
	});
        
	grid.addMouseMotionListener(new MouseMotionListener() {
		@Override
        public void mouseDragged(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1 && e.getButton() != MouseEvent.NOBUTTON) return;

            panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

            Point currentPoint = e.getPoint(),
                    currentOffset = new Point(currentPoint.x - grid.getMouseDragPoint().x, currentPoint.y - grid.getMouseDragPoint().y);

            if (grid.getGridOffset().x >= (model.getGridSize() * model.getBlockSize()) / 2 && currentOffset.x > 0)
                currentOffset.setLocation(0, currentOffset.y);
            if (grid.getGridOffset().x <= -(model.getGridSize() * model.getBlockSize()) / 2 && currentOffset.x < 0)
                currentOffset.setLocation(0, currentOffset.y);

            if (grid.getGridOffset().y >= (model.getGridHeight() * model.getBlockSize()) / 2 && currentOffset.y > 0)
                currentOffset.setLocation(currentOffset.x, 0);
            if (grid.getGridOffset().y <= -(model.getGridHeight() * model.getBlockSize()) / 2 && currentOffset.y < 0)
                currentOffset.setLocation(currentOffset.x, 0);

            grid.setGridOffset(new Point(grid.getGridOffset().x + currentOffset.x, grid.getGridOffset().y + currentOffset.y));
            grid.setMouseDragPoint(e.getPoint());
        }
		
        @Override
        public void mouseMoved(MouseEvent e) {
        }
        
	});
	}

	 public void Initialize(){
	        panel.setPattern(pref.getInt("Pattern", 0));
	        panel.setSize(pref.getInt("Zoom", 1));
	        panel.setSpeed(pref.getInt("Speed", 1));

	        if (pref.getBoolean("restore", false)) {
	            boolean[] gridPref = getGridPref();
	            for (int i = 0; i < GridModel.GRIDSIZE; i++) {
	                model.getAllCells()[i].setAlive(gridPref[i]);
	            }
	        }
	    }

	    public byte[] getCellsByteArray() {
	        boolean[] cellGrid = new boolean[model.getAllCells().length];

	        for (int i = 0; i < model.getAllCells().length; i++) {
	            cellGrid[i] = model.getAllCells()[i].isAlive();
	        }

	        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	        try {
	            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
	            objStream.writeObject(cellGrid);
	            objStream.flush();
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return byteStream.toByteArray();
	    }

	    public boolean[] getGridPref() {
	        boolean[] gridData = new boolean[GridModel.GRIDSIZE];
	        byte[] data = pref.getByteArray("grid", null);
	        if (data != null) {
	            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
	            try {
	                ObjectInputStream objStream = new ObjectInputStream(byteStream);
	                gridData = (boolean[]) objStream.readObject();
	            } catch (IOException | ClassNotFoundException e) {
	                e.printStackTrace();
	            }
	        }
	        return gridData;
	    }

	public void updateSize() {
		int gridHeight = model.GRIDHEIGHT;
		int gridWidth = model.GRIDWIDTH;
		int blockSize = grid.getHeight() / gridHeight;
		
		if(panel.getCurrentSize() == 1) {
			blockSize = blockSize * 2;
		}
		if(panel.getCurrentSize() == 2) {
			blockSize = blockSize * 4;
		}
		
		model.setGridHeight(gridHeight);
		model.setGridSize(gridWidth);
		model.setBlockSize(blockSize);	
		
   
		if (!start) {
            Initialize();
            start = true;
        }
	}
	
	public void updateGame() {
	       for (AliveCell cell : model.getAllCells()) {
	    	   cell.updateCell();
	    	   cell.updateAliveState();
	       }
	       panel.repaint();
	}
		
	@Override
    public void run() {
    	while(true) {
    		updateSize();
	        panel.setGenerationText("Generation: " + model.getGeneration());
    	}
    }	 
}

class GridModel {
	public static final int GRIDSIZE = 5000, GRIDHEIGHT = 50, GRIDWIDTH = GRIDSIZE / GRIDHEIGHT;
	private int generation = 0, gridSize, blockSize, gridHeight, currentSpeed = 300;
	private String[] patterns = {"Clear", "Blinker", "Block", "Tub", "Boat", "Glider", "Ship", "Beehive", "Barge", "Toad", "Beacon", "Long Boat", "Loaf", "Pond", "Mango", "Long Barge", "Half-Fleet", "Half-Bakery"};
	private String[] speeds = {"Slow", "Normal", "Fast"};
	private String[] sizes = {"Small", "Medium", "Big"};
	private AliveCell[] cells_array;
	
	public void increaseGeneration() {
		generation++;
	}
	
	public void setCurrentAge(int ge) {
		this.generation = ge;
	}
	
	public void setSpeed(int sp) {
		this.currentSpeed = sp;
	}
	
	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}
	
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}
	
	public void setGridHeight(int height) {
		this.gridHeight = height;
	}
	
	public int getGeneration() {
		return generation;
	}
	
	public int getBlockSize() {
		return blockSize;
	}
	
	public int getGridSize() {
		return gridSize;
	}
	
	public int getGridHeight() {
		return gridHeight;
	}
	
	public String[] getPatterns() {
		return patterns;
	}
	
	public String[] getSpeeds() {
		return speeds;
	}
	
	public int getSpeed() {
		return currentSpeed;
	}
	
	public String[] getSizes() {
		return sizes;
	}
	
	public AliveCell getCell(int i) { 
		return cells_array[i]; 
	}
	
	public AliveCell[] getAllCells() { 
		return cells_array; 
	}
	
	void setGeneration(int ge) {
		this.generation = ge;
	}
	
	void setAllCells(AliveCell[] cells_array) {
		this.cells_array = cells_array;
	}
	
}

//@SuppressWarnings("serial")
class GridPanel extends JPanel {
	private GridModel model;
	private Point mouseDragPoint;
	private Point gridOffset = new Point(0, 0);
	
	GridPanel(GridModel model) {
		this.model = model;
		this.setBackground(Color.BLACK);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		int Xcorrection = (this.getWidth() / 2) - ((model.getGridSize() * model.getBlockSize()) / 2) + gridOffset.x;
		int Ycorrection = (this.getHeight() / 2) - ((model.getGridHeight() * model.getBlockSize()) / 2) + gridOffset.y;
		
		for (int i = 0; i < model.getGridSize() ; i++) {
            for (int j = 0; j < model.getGridHeight(); j++) {
                int index = i + j * model.GRIDWIDTH;
                g2.setColor(Color.BLUE);

                if (model.getCell(index).isAlive()) {
                  g2.setColor(Color.YELLOW);
                }

                int Xposition = i * model.getBlockSize() + Xcorrection;
                int Yposition = j * model.getBlockSize() + Ycorrection;
                g2.fillRect(Xposition + 1, Yposition + 1, model.getBlockSize() - 5, model.getBlockSize() - 5);

                if (model.getBlockSize() >= 7) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawRect(Xposition + 1, Yposition + 1, model.getBlockSize() - 5, model.getBlockSize() - 5);
                }
            }
        }		
	}
	
	public void setGridOffset(Point offset) { 
		this.gridOffset = offset; 
	}
    
    public void setMouseDragPoint(Point mouseDragPoint) { 
    	this.mouseDragPoint = mouseDragPoint; 
    }
    
    public Point getGridOffset() { 
    	return this.gridOffset; 
    }
    
    public Point getMouseDragPoint() { 
    	return this.mouseDragPoint; 
    }
}

class GameView extends JPanel implements Runnable {
	private JPanel ControlsPanel, PrefPanel, PrefCenter, PrefSouth; 
	
	private JButton prefSubmit,next, start;
	private JCheckBox edit,savePref;
	private JComboBox<String> patterns, speeds, sizes, prefPattern, prefSpeed, prefSize;
	private JLabel generation;
	private JDialog prefDialog;
	
	public void createGridPanel(GridPanel grid) {	
		
		//Display and control panel
		
		setLayout(new BorderLayout());
		edit = new JCheckBox("Edit Mode");
		
		next = new JButton("Next");
		start = new JButton("Start");
		
		generation = new JLabel("Generation: 0");
		generation.setForeground(Color.WHITE);
		
		ControlsPanel = new JPanel();
		ControlsPanel.setLayout(new FlowLayout());
		ControlsPanel.add(edit);
		ControlsPanel.add(next);
		ControlsPanel.add(start);
		ControlsPanel.add(patterns);
		ControlsPanel.add(speeds);
		ControlsPanel.add(sizes);
		ControlsPanel.add(generation);
		ControlsPanel.setBackground(Color.BLACK);
		
		add(grid, BorderLayout.CENTER);
		add(ControlsPanel, BorderLayout.SOUTH);	
		
		//User Custom Preference dialog
        prefDialog = new JDialog(getTopFrame(), "User Preferences", true);

        PrefPanel = new JPanel();
        PrefCenter = new JPanel();
        PrefSouth = new JPanel();
        PrefPanel.setLayout(new BorderLayout());
        PrefCenter.setLayout(new GridLayout(3, 2));
        PrefSouth.setLayout(new FlowLayout());
        
        PrefCenter.add(new JLabel("Starting Pattern="));
        PrefCenter.add(prefPattern);
        PrefCenter.add(new JLabel("Stimulation Speed="));
        PrefCenter.add(prefSpeed);
        PrefCenter.add(new JLabel("Zoom Factor="));
        PrefCenter.add(prefSize);
        
        savePref = new JCheckBox("Save custom setting");
        PrefSouth.add(savePref);
        prefSubmit = new JButton("Submit");
        PrefSouth.add(prefSubmit);
        
        PrefPanel.setBorder(new EmptyBorder(20, 20, 5, 20));
        PrefPanel.add(PrefCenter,BorderLayout.CENTER);
        PrefPanel.add(PrefSouth, BorderLayout.SOUTH);
        prefDialog.add(PrefPanel);
        prefDialog.setResizable(false);
        
        prefDialog.pack();
	}
	
	@Override
	public void run() {
		while(true) {
			
	        repaint();
	        try {
				Thread.sleep(15);
			} 
	        catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setGenerationText(String string) {
		generation.setText(string);
	}

	public void setPatterns(String[] strings) {
		patterns = new JComboBox<String>(strings);
	}
	
	public void setPrefPatterns(String[] strings) {
		prefPattern = new JComboBox<String>(strings);
	}
	
	public void setPattern(int i) {
		patterns.setSelectedIndex(i);
	}
	
	public void setPrefPattern(int i) {
		prefPattern.setSelectedIndex(i);
	}
	
	public void setSpeeds(String[] strings) {
		speeds = new JComboBox<String>(strings);
	}
	
	public void setPrefSpeeds(String[] strings) {
		prefSpeed = new JComboBox<String>(strings);
	}
	
	public void setSpeed(int i) {
		speeds.setSelectedIndex(i);
	}
	
	public void setPrefSpeed(int i) {
		prefSpeed.setSelectedIndex(i);
	}
	
	public void setSizes(String[] strings) {
		sizes = new JComboBox<String>(strings);
	}
	
	public void setPrefSizes(String[] strings) {
		prefSize = new JComboBox<String>(strings);
	}
	
	public void setSize(int i) {
		sizes.setSelectedIndex(i);
	}
	 
	public void setPrefSize(int i) {
		sizes.setSelectedIndex(i);
	}
	
	public void setPrefsDialogVisible(boolean visible) {
		prefDialog.setVisible(visible); 
	} 
	
	public void addSubmitListener(ActionListener listener) { 
		prefSubmit.addActionListener(listener); 
	}
	
	public void addNextListener(ActionListener listener) { 
		next.addActionListener(listener); 
	}
	 
	public void addStartListener(ActionListener listener) { 
		start.addActionListener(listener); 
	}
	 
	public void addSizesListener(ActionListener listener) {
		sizes.addActionListener(listener); 
	}
	 
	public void addSpeedsListener(ActionListener listener) {
		speeds.addActionListener(listener);
	}
	
	public void addPatternListListener(ActionListener listener) {
		patterns.addActionListener(listener); 
	}
	
	public void setNextBoolean(boolean enabled) {
		next.setEnabled(enabled);
	}
	
	public void setStartText(String str) {
		start.setText(str);
	}
	
	public JFrame getTopFrame() {
        return (JFrame) SwingUtilities.getWindowAncestor(this);
    }
	
	public int getCurrentPattern() { 
		return patterns.getSelectedIndex();
	}
	
	public int getCurrentSpeed() {
		return speeds.getSelectedIndex();
	}
	 
	public int getCurrentSize() {
		return sizes.getSelectedIndex();
	}
	
    public int getSelectedPrefPattern() { 
    	return prefPattern.getSelectedIndex(); 
    }
    
    public int getSelectedPrefSpeed() { 
    	return prefSpeed.getSelectedIndex(); 
    }
    
    public int getSelectedPrefSize() { 
    	return prefSize.getSelectedIndex(); 
    }
	
	public String getPatternListSelectedString() { 
		return patterns.getSelectedItem().toString(); 
	}
	
	public void setEditable(boolean editable) {
		edit.setSelected(editable);
	}
	
	public boolean isEditable() {
		return edit.isSelected();
	}
	
    public void setSavePreference(boolean selected) { 
    	savePref.setSelected(selected); 
    }
    
    public boolean isSavePref() {
    	return savePref.isSelected();
    }
   
}


class AliveCell implements Serializable{
	  private static final long serialVersionUID = 2L;
    private int XCord, YCord;
    private boolean alive = false;
    private boolean nextAliveCells = false; 
    private AliveCell[] cells_array;

    AliveCell(int currentIndex, AliveCell[] cells) {
        this.XCord = currentIndex % GridModel.GRIDWIDTH;
        this.YCord = currentIndex / GridModel.GRIDWIDTH;
        this.cells_array = cells;
    }


    public int getIndex(int x, int y) {
        return x + y * GridModel.GRIDWIDTH;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = this.nextAliveCells = alive;
    }

    public void updateAliveState() {
        this.alive = this.nextAliveCells;
    }

    //laws of the game, determines how the cells behave
    public void updateCell() {
        int neighbouringCells = 0;

        if (XCord < GridModel.GRIDWIDTH - 1) {
            if (cells_array[getIndex(XCord + 1, YCord)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (XCord > 0) {
            if (cells_array[getIndex(XCord - 1, YCord)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord < GridModel.GRIDHEIGHT - 1) {
            if (cells_array[getIndex(XCord, YCord + 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord > 0) {
            if (cells_array[getIndex(XCord, YCord - 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord > 0 && XCord > 0) {
            if (cells_array[getIndex(XCord - 1, YCord - 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord < GridModel.GRIDHEIGHT - 1 && XCord < GridModel.GRIDWIDTH - 1) {
            if (cells_array[getIndex(XCord + 1, YCord + 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord > 0 && XCord < GridModel.GRIDWIDTH - 1) {
            if (cells_array[getIndex(XCord + 1, YCord - 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (YCord < GridModel.GRIDHEIGHT - 1 && XCord > 0) {
            if (cells_array[getIndex(XCord - 1, YCord + 1)].isAlive()) {
            	neighbouringCells++;
            }
        }

        if (alive) {
            nextAliveCells = neighbouringCells == 2 || neighbouringCells == 3;
        } else {
            nextAliveCells = neighbouringCells == 3;
        }
    }
}

class GridSetup implements Serializable{
	
	private static final long serialVersionUID = 2L;
	private int Age, Speed, Size, InitialP;
	private boolean Editability;
	private Point gridOffset;
	private AliveCell[] cells_array;

	GridSetup(AliveCell[] cells, int Ge, int Speed, int Size, int InitialP, boolean editMode, Point gridOffset) {
		this.cells_array = cells;
		this.Age = Ge;
		this.Speed = Speed;
		this.Size = Size;
		this.InitialP = InitialP;
		this.Editability = editMode;
		this.gridOffset = gridOffset;
	}

	public int getGeneration() {
		return Age;
	}

	public int getSpeed() {
		return Speed;
	}	

	public int getSize() {
		return Size;
	}

	public int getStartingPattern() {
		return InitialP;
	}

	public boolean getEditMode() {
		return Editability;
	}

	public Point getGridOffset() {
		return gridOffset;
	}

	public AliveCell[] getCells() {
		return cells_array;
	}
}
