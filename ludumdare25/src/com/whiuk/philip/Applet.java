package com.whiuk.philip;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class Applet extends java.applet.Applet implements KeyListener, MouseListener {	

	public interface Updateable {
		public void update();
	}
	private interface Renderable {
		public void render(Graphics2D g);		
	}
	private interface Moveable {
		void moveLeft(boolean move);
		void moveRight(boolean move);
		void moveUp(boolean move);
		void moveDown(boolean move);
		void setPosition(int playerStartPosX, int playerStartPosY);
	}
	private abstract class Character extends Rectangle implements Renderable, Updateable {
		private Color color;
		protected Room room;
		public Character(int x, int y,Color color, Room r) {
			super(x,y,CHAR_WIDTH,CHAR_HEIGHT);
			this.color = color;
			this.room = r;
		}
		
		@Override
		abstract public void update();
		
		public void render(Graphics2D g) {			
			g.setColor(color);
			g.fill(this);
		}		
		public void setRoom(Room room) {
			this.room = room;
		}		
	}
	
	private class Player extends Character implements Moveable {
		private boolean moveLeft, moveRight, moveUp, moveDown;

		public Player() {
			super(PLAYER_START_POS_X,PLAYER_START_POS_Y,PLAYER_COLOR,null);
		}
		public void moveUp(boolean move) {
			moveUp = move;
		}

		public void moveDown(boolean move) {
			moveDown = move;
		}
		
		@Override
		public void moveLeft(boolean move) {
			moveLeft = move;
		}

		@Override
		public void moveRight(boolean move) {
			moveRight = move;
		}
		public void update() {
			int xPrev = x;
			int yPrev = y;
			
			if(moveLeft) {
				x -= PLAYER_MOVE_SPEED;
			}
			if(moveRight) {
				x += PLAYER_MOVE_SPEED;
			}
			if(moveUp) {
				y -= PLAYER_MOVE_SPEED;
			}
			if(moveDown) {
				y += PLAYER_MOVE_SPEED;
			}			
						
			x = x < 0 ? 0 : x;
			x = x > GAME_WIDTH ? GAME_WIDTH: x;
			y = y < 0 ? 0 : y;
			y = y > GAME_HEIGHT ? GAME_HEIGHT: y;			
			
			for(Obstacle ob: room.getObstacles()) {			
				if(player.intersects(ob)) {
					x = xPrev;
					y = yPrev;
				}
			}
			boolean doorIntersection = false;
			for(Door d: room.getDoors()) {	
				//For the case where the player intercepts the door.
				if(intersects(d)) {
					doorIntersection = true;
					if(!d.open) {
						x = xPrev;
						y = yPrev;
					}
					else {
						d.r1.visible = true;
						d.r2.visible = true;
					}
					break;
				} else if(d.open) {
					if(d.r1 != room) {
						if(intersects(d.r1) && !intersects(room)) {
							doorIntersection = true;
							room.exit();
							d.r1.enter();
							break;
						}
					}
					else if (d.r2 != room) {
						if(intersects(d.r2) && !intersects(room)) {
							doorIntersection = true;
							room.exit();
							d.r2.enter();
							break;							
						}
					}
				}
			}
			
			if(!player.intersects(room) && !doorIntersection) {
				x = xPrev;
				y = yPrev;
			}
			
		}
		@Override
		public void setPosition(int x, int y) {
			this.x = x;
			this.y = y;
		}
		public void performAction() {
			switch(currentAction) {
				case COMPLETE_LEVEL:
					if(room.isGoal()) {
						finishLevel();
						levelUp();
					}
					break;
			}
		}
		private void finishLevel() {
			// TODO Auto-generated method stub
			
		}
		public void startFiring() {
			// TODO Auto-generated method stub
			
		}

		
		
	}
	private class Enemy extends Character {

		public Enemy(Room r) {			
			super((int)r.getCenterX(),(int)r.getCenterY(), ENEMY_COLOR, r);
		}
		@Override
		public void update() {
			int xPrev = x;
			int yPrev = y;
			
			int moveX = random.nextInt(7)-3;
			int moveY = random.nextInt(7)-3;
			x += (moveX*5);
			y += (moveY*5);
			
			x = x < 0 ? 0 : x;
			x = x > GAME_WIDTH ? GAME_WIDTH: x;
			y = y < 0 ? 0 : y;
			y = y > GAME_HEIGHT ? GAME_HEIGHT: y;
			
			if(!this.intersects(room)) {
				x = xPrev;
				y = yPrev;
			}
			
		}
		
	}
	private class Obstacle extends Rectangle implements Renderable {
		public Obstacle(int x, int y, int width, int height) {
			super(x,y,CHAR_WIDTH,CHAR_HEIGHT); 
		}

		@Override
		public void render(Graphics2D g) {
			g.setColor(WALL_COLOR);
			g.fill(this);
		}
	}
	private class Door extends Rectangle implements Renderable {
		private boolean open = true;
		private Path path;
		public Room r1, r2;		

		public Door(Path path,Room room1, Room room2) {
			if(path.equals(Path.NORTHSOUTH)) {
				this.x = room2.x+12;
				this.y = room2.y-5;
				this.width = 50;
				this.height = 10;
			} else {
				this.x = room2.x-5;
				this.y = room2.y+10;
				this.height = 50;
				this.width = 10;
			}
			this.path = path;
			r1 = room1;
			r2 = room2;
		}

		@Override
		public void render(Graphics2D g) {
			if(open) {
				g.setColor(FLOOR_COLOR);
				g.fill(this);
			} else {
				g.setColor(DOOR_COLOR);
				g.fill(this);
			}
		}
	}
	private class Room extends Rectangle implements Renderable {
		private int xCol, yCol;
		private boolean visible;
		private boolean active;
		private ArrayList<Obstacle> obstacles;
		private ArrayList<Enemy> enemies;		
		private HashMap<Direction,Door> doors;
		private boolean goal;
		public Room(int xCol, int yCol) {
			super(xCol*(ROOM_WIDTH+ROOM_SPACING),yCol*(ROOM_HEIGHT+ROOM_SPACING),ROOM_WIDTH,ROOM_HEIGHT);
			this.xCol = xCol;
			this.yCol = yCol;
			doors = new HashMap<Direction,Door>();
			obstacles = new ArrayList<Obstacle>();
			enemies = new ArrayList<Enemy>();			
			visible = false;
		}
		public Room(Door d) {			
			doors = new HashMap<Direction,Door>();
			obstacles = new ArrayList<Obstacle>();			
		}
		void generateDoors() {
			if(yCol < ROOMS_Y-1) {
				if(random.nextBoolean()) { 				
					Door d = new Door(Path.NORTHSOUTH,this,rooms[xCol][yCol+1]);
					doors.put(Direction.SOUTH,d);
					rooms[xCol][yCol+1].addDoor(Direction.NORTH,d);
				} 
			}
			if(xCol < ROOMS_X-1) {
				if(random.nextBoolean()) { 				
					Door d = new Door(Path.EASTWEST,this,rooms[xCol+1][yCol]);
					doors.put(Direction.WEST,d);
					rooms[xCol+1][yCol].addDoor(Direction.EAST,d);
				} 
			} 						
		}
		public ArrayList<Obstacle> getObstacles() {
			return this.obstacles;
		}
		public Collection<Door> getDoors() {
			return this.doors.values();
		}
		void enter() {
			visible = true;
			active = true;
			player.setRoom(this);
			logger.info("Entered room:"+xCol+","+yCol);
		}
		void exit() {
			active = false;
		}
		void addDoor(Direction dir, Door d) {
			doors.put(dir,d);
		}
		void addEnemy(Enemy e) {
			enemies.add(e);
		}
		
		@Override
		public void render(Graphics2D g) {
			if (goal && active) {
				g.setColor(GOAL_COLOR);
			}
			else if (active) {
				g.setColor(FLOOR_COLOR);
			}
			else if (goal && visible) {
				g.setColor(FOW_GOAL_COLOR);
			}
			else if (visible) {
				g.setColor(FOW_FLOOR_COLOR);
			}
			if(active || visible) {
				g.fill(this);				
				for(Obstacle obs: obstacles) {
					obs.render(g);
				}
				for(Door door: doors.values()) {
					door.render(g);
				}
			}
			if(active) {
				for(Enemy e: enemies) {
					e.render(g);
				}					
			}
		}
		private boolean hasDoor(Direction direction) {
			return doors.containsKey(direction);
		}
		public void setGoal() {
			this.goal = true;
		}
		public boolean isGoal() {
			return goal;
		}

		
	}

	private class GameThread extends Thread {
		
		public GameThread() {
			super("GameThread");
		}
		
		public void run() {
			score = 0;
			level = 0;
			currentAction = Action.COMPLETE_LEVEL;
			player = new Player();
			enemies = new ArrayList<Enemy>();
            gameTick();
                        
	        final double GAME_HERTZ = 30.0;
		    final double TIME_BETWEEN_UPDATES = 1000000000 / GAME_HERTZ;
		    final int MAX_UPDATES_BEFORE_RENDER = 5;
		    double lastUpdateTime = System.nanoTime();
		    double lastRenderTime = System.nanoTime();
		    final double TARGET_FPS = 60;
		    final double TARGET_TIME_BETWEEN_RENDERS = 1000000000 / TARGET_FPS;
            int lastSecondTime = (int) (lastUpdateTime / 1000000000);          
			while(gameState.equals(GameState.GAME_IN_PROGRESS) || gameState.equals(GameState.GAME_PAUSED)) {
				double now = System.nanoTime();
				int updateCount = 0;
				if (!gameState.equals(GameState.GAME_PAUSED)) {
					while( now - lastUpdateTime > TIME_BETWEEN_UPDATES && updateCount < MAX_UPDATES_BEFORE_RENDER ) {
						gameTick();
						lastUpdateTime += TIME_BETWEEN_UPDATES;
						updateCount++;
					}
					if ( now - lastUpdateTime > TIME_BETWEEN_UPDATES)
		            {
		               lastUpdateTime = now - TIME_BETWEEN_UPDATES;
		            }
					float interpolation = Math.min(1.0f, (float) ((now - lastUpdateTime) / TIME_BETWEEN_UPDATES) );
					gameRepaint(interpolation);
		            lastRenderTime = now;
		            int thisSecond = (int) (lastUpdateTime / 1000000000);
		            if (thisSecond > lastSecondTime)
		            {
		               fps = frameCount;
		               frameCount = 0;
		               lastSecondTime = thisSecond;
		            }
		            while ( now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS && now - lastUpdateTime < TIME_BETWEEN_UPDATES)
		            {
		               Thread.yield();
		               try {Thread.sleep(1);} catch(Exception e) {}
		               now = System.nanoTime();
		            }
				}
 				gameTick();
				repaint();				
			}
			
		}
	}
	private class Score {
		String name;
		int score;
	}
	private class Flashlight implements Shape, Renderable {
		@Override
		public void render(Graphics2D g) {
			g.setColor(FLASHLIGHT_COLOR);
			g.fill(this);
		}

		@Override
		public Rectangle getBounds() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Rectangle2D getBounds2D() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean contains(double x, double y) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean contains(Point2D p) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean intersects(double x, double y, double w, double h) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean intersects(Rectangle2D r) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean contains(double x, double y, double w, double h) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean contains(Rectangle2D r) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public PathIterator getPathIterator(AffineTransform at) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public PathIterator getPathIterator(AffineTransform at, double flatness) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	private enum GameState{GAME_IN_PROGRESS,GAME_PAUSED,GAME_END,HIGHSCORES,CREDITS,MAIN};
	private enum Path {NORTHSOUTH,EASTWEST};
	private enum Direction {NORTH,EAST,SOUTH,WEST};
	private enum Action {COMPLETE_LEVEL};
	
	private static final Font TITLE_FONT = new Font("Lucida Sans Unicode", Font.PLAIN, 24);

	private static final String NEW_GAME_TXT = "New Game (N)";
	private static final int NEW_GAME_TXT_X = 100;
	private static final int NEW_GAME_TXT_Y = 200;
	private static final String HIGHSCORES_TXT = "Highscores (I)";
	private static final int HIGHSCORES_TXT_X = 100;
	private static final int HIGHSCORES_TXT_Y = 300;
	private static final String TITLE_TXT = "Ludum Dare 25";
	private static final int TITLE_TXT_X = 250;
	private static final int TITLE_TXT_Y = 50;

	private static final int GAME_WIDTH = 1024;
	private static final int GAME_HEIGHT = 700;
	
	private static final Color BACKGROUND_COLOR = Color.BLACK;
	private static final Color PLAYER_COLOR = Color.WHITE;
	private static final Color FOW_FLOOR_COLOR = Color.DARK_GRAY;
	private static final Color ENEMY_COLOR = Color.BLUE;
	private static final Color WALL_COLOR = Color.GRAY;
	private static final Color DOOR_COLOR = Color.BLUE;
	private static final Color GOAL_COLOR = Color.YELLOW;	
	private static final Color FOW_GOAL_COLOR = Color.getHSBColor(((float)62/360), ((float)48/100), ((float)35/100));
	private static final Color FLOOR_COLOR = Color.lightGray;
	private static final Color FLASHLIGHT_COLOR = Color.yellow;
	
	private static final int PLAYER_START_POS_X = 50;
	private static final int PLAYER_START_POS_Y = 50;	
	private static final int PLAYER_MOVE_SPEED = 7;		
	
	public static final int ROOM_WIDTH = 100;
	public static final int ROOM_HEIGHT = 100;
	public static final int ROOMS_X = 8;
	public static final int ROOMS_Y = 4;
	public static final int ROOM_SPACING = 5;
	
	
	private Image offImage;
	private Graphics offGraphics;

	private ArrayList<Enemy> enemies;
	private Room[][] rooms;
	private Player player;
	private Action currentAction;
	
	private int score; 
	private int level;

	
	private Score[] highscores = new Score[10];
	private Logger logger = Logger.getLogger(Applet.class);
	private GameThread gameThread;
	private GameState gameState;
	private int fps = 60;
	private int frameCount = 0;
	private float interpolation;
	private Random random;

	public static int CHAR_WIDTH = 10;
	public static int CHAR_HEIGHT = 10;	
	
	//Applet
	public void init() {
		BasicConfigurator.configure();
		gameState = GameState.MAIN;
		random = new Random();
		addKeyListener(this);
		addMouseListener(this);
	}
	public void start() {
		
	}
	public void stop() { 
		
	}
	public void destroy() {
		
	}
	
	//Drawing
	public void update(Graphics g) {
		// create buffer 
		if (offImage == null) 
		{
			offImage = createImage (this.getSize().width, this.getSize().height); 
			offGraphics = offImage.getGraphics (); 
		} 

		// Set the background 
		offGraphics.setColor (BACKGROUND_COLOR); 
		offGraphics.fillRect (0, 0, this.getSize().width, this.getSize().height); 

		// draw elements 
		offGraphics.setColor (getForeground()); 
		paint (offGraphics); 

		// draw image on the screen 
		g.drawImage (offImage, 0, 0, this); 
	}	
	public void paint(Graphics g) {
		Graphics2D g2D = (Graphics2D) g;
		switch(gameState) {
			case GAME_IN_PROGRESS:		
				renderGame(g2D);
				break;				
			case GAME_END:
				renderGameEnd(g2D);
				break;
			case HIGHSCORES:
				renderHighscores(g2D);
				break;
			case CREDITS:
				renderCredits(g2D);
				break;
			case MAIN:
				renderMainScreen(g2D);
			default:
				break;
		}
	}
	//Key Handling
	@Override
	public void keyTyped(KeyEvent e) {
		if(gameState.equals(GameState.GAME_IN_PROGRESS)) {
			gameKeyTyped(e);
		}
		else {
			switch(e.getKeyChar()) {
			case 'n':
			case 'N':
				newGame();
				break;
			case 'c':
			case 'C':
				credits();
				break;
			case 'i':
			case 'I':
				highscores();
				break;
			default:
				logger.info("Key Char: "+e.getKeyChar()+" undefined");					
			}
		}
	}
	@Override
	public void keyPressed(KeyEvent e) {
		if(gameState.equals(GameState.GAME_IN_PROGRESS)) {
			gameKeyPressed(e);
		}
		else {
			int kc = e.getKeyCode();
			switch(kc) {
				case KeyEvent.VK_N:
					newGame();
					break;
			default:
				logger.info("MAIN: Key Event undefined");
			}					
		}			
	}
	@Override
	public void keyReleased(KeyEvent e) {
		if(gameState.equals(GameState.GAME_IN_PROGRESS)) {
			gameKeyReleased(e);
		}
		else {
			int kc = e.getKeyCode();
			switch(kc) {
			default:
				logger.info("Key Event undefined");
			}					
		}	
	}

	//Mouse Handling
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}	
	
	//Render
	private void renderGame(Graphics2D g) {
		synchronized(rooms) {
			for (int x = 0; x < ROOMS_X; x++) {
				for (int y = 0; y < ROOMS_Y; y++) {
					rooms[x][y].render(g);
				}
			}
		}
		player.render(g);
	}
	private void renderMainScreen(Graphics2D g) {
		g.setFont(TITLE_FONT);
		g.drawString(TITLE_TXT, TITLE_TXT_X, TITLE_TXT_Y);
		g.drawString(NEW_GAME_TXT, NEW_GAME_TXT_X, NEW_GAME_TXT_Y);
		g.drawString(HIGHSCORES_TXT, HIGHSCORES_TXT_X, HIGHSCORES_TXT_Y);		
		
	}
	private void renderHighscores(Graphics2D g) {
		for(int i = 0; i < highscores.length; i++) {
			//TODO: render highscores
			//renderScore(g,highscores[i]);
		}
	}
	private void renderGameEnd(Graphics2D g) {
		//TODO: render end game
	}
	private void renderCredits(Graphics2D g) {
		//TODO: render credits
	}
	
	//Game Keys
	private void gameKeyPressed(KeyEvent e) {
		int kc = e.getKeyCode();		
		switch(kc) {
			case KeyEvent.VK_W:
				player.moveUp(true);
				break;
			case KeyEvent.VK_A:
				player.moveLeft(true);
				break;
			case KeyEvent.VK_D:
				player.moveRight(true);
				break;
			case KeyEvent.VK_S:
				player.moveDown(true);				
				break;
			case KeyEvent.VK_SPACE:
				player.startFiring();
				break;
			case KeyEvent.VK_ENTER:
				player.performAction();				
			default:
				logger.info("[GAME] Key Event undefined");
					
		}
	}
	private void gameKeyReleased(KeyEvent e) {
		int kc = e.getKeyCode();		
		switch(kc) {
			case KeyEvent.VK_W:
				player.moveUp(false);
			break;
			case KeyEvent.VK_A:
				player.moveLeft(false);
				break;
			case KeyEvent.VK_D:
				player.moveRight(false);
				break;
			case KeyEvent.VK_S:
				player.moveDown(false);
				break;
			default:
				logger.info("[GAME] Key Event undefined");
					
		}
	}
	private void gameKeyTyped(KeyEvent e) {
		
	}

	//Game Life-cycle
	private void newGame() {
		synchronized(gameState) {
			gameThread = new GameThread();
			gameThread.start();
			gameState = GameState.GAME_IN_PROGRESS;
		}
	}
	private void highscores() {
		gameState = GameState.HIGHSCORES;		
	}
	private void credits() {
		gameState = GameState.CREDITS;		
	}	

	//Game
	private void gameTick() {
		if(enemies.size() == 0) {
			levelUp();
		}		
		for(Enemy e: enemies) {
			e.update();
		}
		player.update();		
	}
	
	private void levelUp() {		
		level++;
		logger.info("[GAME] Generating level "+level);
		buildRooms();
		player.setPosition(PLAYER_START_POS_X,PLAYER_START_POS_Y);
		rooms[0][0].enter();
		spawnEnemies(level*10);					
	}	
	private void buildRooms() {
		ArrayList<Door> doors = new ArrayList<Door>();
		rooms = new Room[ROOMS_X][ROOMS_Y];

		for(int x = 0; x < ROOMS_X; x++) {
			for(int y = 0; y < ROOMS_Y; y++) {
				rooms[x][y] = new Room(x,y);
			}
		}
		spawnGoal();
		while(!pathToGoal()) {
			logger.info("[GAME] Adding door");
			addDoor();
		}
		logger.info("[GAME] Built room layout");
	}
	private boolean pathToGoal() {
		HashSet<Room> expanded = new HashSet<Room>();
		LinkedList<Room> toExpand = new LinkedList<Room>();
		toExpand.add(rooms[0][0]);
		while(toExpand.size() != 0) {	
			Room r = toExpand.pop();
			logger.info("[GAME] Expanding room:"+r.xCol+", "+r.yCol);
			expanded.add(r);
			if(r.isGoal()) {
				return true;
			}
			Collection<Door> doors = r.getDoors();
			for(Door d: doors) {
				if(d.r1 == r) {
					if(!expanded.contains(d.r2)) {
						toExpand.add(d.r2);
					}
				}
				else if(d.r2 == r) {
					if(!expanded.contains(d.r1)) {
						toExpand.add(d.r1);
					}
				}
				else {
					throw new IllegalArgumentException("Door with two unknown locations");
				}
			}			
		}
		String rooms = "";
		for(Room r : expanded) {
			rooms += r.xCol+", "+r.yCol+" ::";
		}
		logger.info("Expanded: "+rooms);
		return false;
	}
	private void spawnGoal() {
		int x = 0,y = 0;
		while (x == 0 && y == 0) {
			x = random.nextInt(ROOMS_X);
			y = random.nextInt(ROOMS_Y);
		}
		rooms[x][y].setGoal();
	}
	private void addDoor() {
		int failures = 0;
		while(failures < 20) {
			int x = random.nextInt(ROOMS_X);
			int y = random.nextInt(ROOMS_Y);
			boolean d = random.nextBoolean();
			if(d && y+1 < ROOMS_Y) {
				Door door = new Door(Path.NORTHSOUTH,rooms[x][y],rooms[x][y+1]);
				if(!rooms[x][y].hasDoor(Direction.SOUTH)) {
					rooms[x][y].addDoor(Direction.SOUTH,door);
					rooms[x][y+1].addDoor(Direction.NORTH,door);
					return;
				}
				logger.info("[GAME] Failed to add door - already exists: "+x+", "+y+" - "+d);
				failures++;
			}
			else if(!d && x+1 < ROOMS_X) {
				Door door = new Door(Path.EASTWEST,rooms[x][y],rooms[x+1][y]);
				if(!rooms[x][y].hasDoor(Direction.WEST)) {
					rooms[x][y].addDoor(Direction.WEST,door);
					rooms[x+1][y].addDoor(Direction.EAST,door);
					return;
				}
				logger.info("[GAME] Failed to add door - already exists: "+x+", "+y+" - "+d);								
				failures++;
			}			
		}
		logger.warn("[GAME] Failed to add door "+failures+" times. Repathing");		
	}
	private void spawnEnemies(int count) {				
		logger.info("[GAME] Spawning enemies");
		for(int i = 0; i < count; i++) {
			int x = 0, y = 0;
			while(x == 0 && y == 0) {
				x = random.nextInt(ROOMS_X);
				y = random.nextInt(ROOMS_Y);
			}
			Enemy e = new Enemy(rooms[x][y]);
			rooms[x][y].addEnemy(e);
			enemies.add(e);
		}
	}
	private void gameRepaint(float interpolation) {
	      setInterpolation(interpolation);
	      repaint();
	}
	private void setInterpolation(float interp) {
       interpolation = interp;
    }	
}
