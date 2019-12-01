package finalproject.Mia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import finalproject.Mia.model.PlayerData;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends JFrame
{
    //variables involving data to send/receiving to/from server
    private int turn;
    private final int lives = 6;
    private int currentScore = 0;
    private int playerNumber;
    private int die1 = 0;
    private int die2 = 0;
    private int bluffValue = 32;
    private String screen;
    private String jsonMessage = "";
    private ObjectMapper objectMapper = new ObjectMapper();
    private PlayerData playerData;
    private JTextField textField;

    //variables involving screen and rendering
    private JPanel jPanel;
    
    private SimpleUniverse myUniverse;
    private BranchGroup myBranchGroup;
    private Canvas3D myCanvas;
    
    public static void main(String[] args)
    {
        Client player1 = new Client();
        player1.setPlayerNumber(1);
        player1.setTurn(1);
        player1.loadTitleScreen();

        Client player2 = new Client();
        player2.setPlayerNumber(2);
        player2.setTurn(0);
        player2.loadTitleScreen();
    }

    public Client()
    {

    }

    public void connectToServer()
    {
        try
        {
            //get address of client machine
            InetAddress inetAddress = InetAddress.getLocalHost();
            String clientAddress = inetAddress.getHostAddress();
            System.out.println("Client Address: " + clientAddress);

            String server;
            server = "localhost";

            //create socket to connect to server address on port 4999
            Socket socket = new Socket(server, 4999);

            //open stream to write from file testInput1.txt (not included)
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

            //send entire message to server and close stream
            System.out.println("\n\nSending to server:\t" + jsonMessage);
            printWriter.println(jsonMessage);

            // to read data from the server
            printWriter.flush();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            jsonMessage = bufferedReader.readLine();
            System.out.println("Receiving from server:\t" + jsonMessage);

            //close connection
            printWriter.close();
            bufferedReader.close();

            playerData = objectMapper.readValue(jsonMessage, PlayerData.class);
            System.out.println("New playerData = \t" + playerData.toString());

            //load screen
            loadScreen(playerData.getScreen());
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        
    }

    public void loadTitleScreen()
    {
        setTitle("Mia - " + getPlayerNumber());
        setScreen("title");
        jsonMessage = "{\"playerNumber\":\"" + getPlayerNumber() + "\", \"turn\":\"" + getTurn() +
                "\", \"lives\":\"" + getLives() + "\", \"currentScore\":\"" + getCurrentScore() +
                "\", \"screen\":\"" + getScreen() + "\"}";
        jPanel = new JPanel();
        initialize(jPanel);
        getContentPane().add(jPanel, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    private void clearPanel()
    {
    	jPanel.removeAll();
    	
    }
    
    public void initialize(JPanel jPanel)
    {
        jPanel.setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        myCanvas = new Canvas3D(config);
        myCanvas.setSize(400, 400);
        myUniverse = new SimpleUniverse(myCanvas);
        
        myBranchGroup = createTitleScreenBranchGroup();
        
        myUniverse.getViewingPlatform().setNominalViewingTransform();
        myUniverse.addBranchGraph(myBranchGroup);
        jPanel.add(myCanvas, BorderLayout.CENTER);

        JButton startBtn = new JButton("Start Game");
        startBtn.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent m)
            {
                connectToServer();
            }

        });
        jPanel.add(startBtn, BorderLayout.SOUTH);
    }
    
    private BranchGroup createTitleScreenBranchGroup()
    {
    	BranchGroup branchGroup = new BranchGroup();
    	addTitleText(branchGroup);
    	addLights(branchGroup);
    	branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
    	return branchGroup;
    }
    
    private BranchGroup createWaitingScreenBranchGroup()
    {
    	BranchGroup branchGroup = new BranchGroup();
        addWaitingText(branchGroup);
        addLights(branchGroup);
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
    	return branchGroup;
    }

    private BranchGroup createPlayingScreenBranchGroup()
    {
    	BranchGroup branchGroup = createPlayScreen();
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        return branchGroup;
    }
    
    private BranchGroup createShowDiceScreenBranchGroup()
    {
    	BranchGroup branchGroup = createShowDiceScreen();
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        return branchGroup;
    }
    
    private BranchGroup createGuessScreenBranchGroup()
    {
    	BranchGroup branchGroup = new BranchGroup();
    	addGuessingText(branchGroup);
    	addLights(branchGroup);
        branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        return branchGroup;
    }
    
    public void loadScreen(String screen)
    {
    	myBranchGroup.detach();
    	myBranchGroup.removeAllChildren();
    	
        if(screen.equalsIgnoreCase("waiting"))
        {
        	clearPanel();
        	myBranchGroup = createWaitingScreenBranchGroup();
            myUniverse.addBranchGraph(myBranchGroup);
            jPanel.add(myCanvas, BorderLayout.CENTER);
        }
        else if(screen.equalsIgnoreCase("play"))
        {
        	clearPanel();
        	jPanel.setLayout(new BorderLayout());
        	
        	myBranchGroup = createPlayingScreenBranchGroup();
            myUniverse.addBranchGraph(myBranchGroup);
            jPanel.add(myCanvas, BorderLayout.CENTER);

            textField = new JTextField(1);
            jPanel.add(textField, BorderLayout.NORTH);
            JButton passBtn = new JButton("Pass Dice");
            passBtn.setVisible(true);
            
            passBtn.addMouseListener(new MouseAdapter()
            {
                public void mouseClicked(MouseEvent m)
                {
                    connectToServer();
                }

            });
            jPanel.add(passBtn, BorderLayout.SOUTH);
            
        }
        else if(screen.equalsIgnoreCase("showDice"))
        {
        	//shows dice
        	clearPanel();
        	myBranchGroup = createShowDiceScreenBranchGroup();
            myUniverse.addBranchGraph(myBranchGroup);
            jPanel.add(myCanvas, BorderLayout.CENTER);
        }
        else if(screen.equalsIgnoreCase("guess"))
        {
        	clearPanel();
        	myBranchGroup = createGuessScreenBranchGroup();
            myUniverse.addBranchGraph(myBranchGroup);
            jPanel.add(myCanvas, BorderLayout.CENTER);
        }
        
        this.revalidate();
        this.repaint();
        
        //this.repaint();
        //getContentPane().add(jPanel, BorderLayout.CENTER);
        //pack();
        //setVisible(true);
    }

    private BranchGroup createPlayScreen() {
        BranchGroup root = new BranchGroup();
        TransformGroup spin = new TransformGroup();
        spin.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        root.addChild(spin);
        //object
        Appearance ap = new Appearance();
        ap.setMaterial(new Material());
        PolygonAttributes pa = new PolygonAttributes();
        pa.setBackFaceNormalFlip(true);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        ap.setPolygonAttributes(pa);
        
        //load texture for cup
        Texture tex = null;
		try {
			tex = new TextureLoader(ImageIO.read( new File("CupTexture.png"))).getTexture();
			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TextureAttributes texAttrib = new TextureAttributes();
		texAttrib.setTextureMode(TextureAttributes.MODULATE);
		if(tex!=null)
		{
			ap.setTexture(tex);
			ap.setTextureAttributes(texAttrib);
		}
        
        Shape3D shape = new Shape3D(MiaShapes.createCup(40,1,2), ap);
        //transformation
        Transform3D tr = new Transform3D();
        tr.setScale(0.4);
        TransformGroup tg = new TransformGroup(tr);
        spin.addChild(tg);
        tg.addChild(shape);
        Alpha alpha = new Alpha(-1, 10000);
        RotationInterpolator rotator = new RotationInterpolator(alpha, spin);
        BoundingSphere bounds = new BoundingSphere();
        bounds.setRadius(100);
        rotator.setSchedulingBounds(bounds);
        spin.addChild(rotator);
        //light
        PointLight light = new PointLight(new Color3f(Color.white),
                new Point3f(0.5f,0.5f,1f),
                new Point3f(1f,0.2f,0f));
        light.setInfluencingBounds(bounds);
        root.addChild(light);
        //background
        Background background = new Background(1.0f, 1.0f, 1.0f);
        background.setApplicationBounds(bounds);
        root.addChild(background);
        
        return root;
    }
    
    private BranchGroup createShowDiceScreen() {
        BranchGroup root = new BranchGroup();
        //object
        Appearance ap = new Appearance();
        ap.setMaterial(new Material());
        PolygonAttributes pa = new PolygonAttributes();
        pa.setBackFaceNormalFlip(true);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        ap.setPolygonAttributes(pa);
        
        //load texture for cup
        Texture tex = null;
		try {
			tex = new TextureLoader(ImageIO.read( new File("DiceTexture.png"))).getTexture();
			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TextureAttributes texAttrib = new TextureAttributes();
		texAttrib.setTextureMode(TextureAttributes.MODULATE);
		if(tex!=null)
		{
			ap.setTexture(tex);
			ap.setTextureAttributes(texAttrib);
		}
        
        Shape3D shape1 = new Shape3D(MiaShapes.createBox(), ap);
        Shape3D shape2 = new Shape3D(MiaShapes.createBox(), ap);
        
        //transformation
        //hard code rotations for the correct side
        Transform3D tr1 = new Transform3D();
        switch(die1)
        {
        case 1:
        	tr1.rotX(Math.PI/2);
        	break;
        case 2:
        	tr1.rotX(3*Math.PI/2);
        	break;
        case 3:
        	tr1.rotY(Math.PI/2);
        	break;
        case 4:
        	tr1.rotY(3*Math.PI/2);
        	break;
        case 5:
        	tr1.rotY(Math.PI);
        	break;
        case 6:
        	break; //no rotation needed
        default:
        	break;
        }
        tr1.setTranslation(new Vector3f(-0.25f, 0, 0));
        tr1.setScale(0.2);
        
        
        
        Transform3D tr2 = new Transform3D();
        switch(die2)
        {
        case 1:
        	tr2.rotX(Math.PI/2);
        	break;
        case 2:
        	tr2.rotX(3*Math.PI/2);
        	break;
        case 3:
        	tr2.rotY(Math.PI/2);
        	break;
        case 4:
        	tr2.rotY(3*Math.PI/2);
        	break;
        case 5:
        	tr2.rotY(Math.PI);
        	break;
        case 6:
        	break; //no rotation needed
        default:
        	break;
        }
        tr2.setTranslation(new Vector3f(0.25f, 0, 0));
        tr2.setScale(0.2);
        
        
        TransformGroup tg1 = new TransformGroup(tr1);
        tg1.addChild(shape1);
        
        TransformGroup tg2 = new TransformGroup(tr2);
        tg2.addChild(shape2);
        
        BoundingSphere bounds = new BoundingSphere();
        bounds.setRadius(100);
        
        //light
        PointLight light = new PointLight(new Color3f(Color.white),
                new Point3f(0.5f,0.5f,1f),
                new Point3f(1f,0.2f,0f));
        light.setInfluencingBounds(bounds);
        root.addChild(light);
        //background
        Background background = new Background(1.0f, 1.0f, 1.0f);
        background.setApplicationBounds(bounds);
        root.addChild(background);
        
        root.addChild(tg1);
        root.addChild(tg2);
        
        return root;
    }

    public void addLights(BranchGroup branchGroup) {
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
                1000.0);
        Color3f light1Color = new Color3f(1.0f, 1.0f, 1.0f);
        Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
        DirectionalLight light1 = new DirectionalLight(light1Color,
                light1Direction);
        light1.setInfluencingBounds(bounds);
        branchGroup.addChild(light1);
        Color3f ambientColor = new Color3f(.1f, .1f, .1f);
        AmbientLight ambientLightNode = new AmbientLight(ambientColor);
        ambientLightNode.setInfluencingBounds(bounds);
        branchGroup.addChild(ambientLightNode);
        Background back = new Background(1, 1, 1);
        back.setApplicationBounds(bounds);
        branchGroup.addChild(back);
    }

    private void addTitleText(BranchGroup branchGroup) {
        Font3D font3D = new Font3D(new Font("Arial", Font.BOLD, 1),
                new FontExtrusion());

        Text3D firstName = new Text3D(font3D, "Welcome to", new Point3f(-2f, 2f, -4.8f));
        firstName.setString("Welcome to");

        Text3D lastName = new Text3D(font3D, "Mia", new Point3f(-.1f, -.3f, -4.8f));
        lastName.setString("Mia");

        Color3f white = new Color3f(1.0f, 1.0f, 1.0f);
        Color3f blueish = new Color3f(.35f, 0.1f, 0.6f);
        Appearance appearance = new Appearance();
        Material material = new Material(blueish, blueish, blueish, white, 82.0f);
        material.setLightingEnable(true);
        appearance.setMaterial(material);

        Shape3D shape3D1 = new Shape3D();
        shape3D1.setGeometry(firstName);
        shape3D1.setAppearance(appearance);

        Shape3D shape3D2 = new Shape3D();
        shape3D2.setGeometry(lastName);
        shape3D2.setAppearance(appearance);

        TransformGroup transformGroup = new TransformGroup();
        Transform3D transform3D = new Transform3D();
        Vector3f v3f = new Vector3f(-1.0f, -1.0f, -4f);
        transform3D.setTranslation(v3f);
        transformGroup.setTransform(transform3D);
        transformGroup.addChild(shape3D1);
        transformGroup.addChild(shape3D2);
        branchGroup.addChild(transformGroup);
    }

    private void addWaitingText(BranchGroup branchGroup) {
        Font3D font3D = new Font3D(new Font("Arial", Font.BOLD, 1),
                new FontExtrusion());

        Text3D firstName = new Text3D(font3D, "WAITING", new Point3f(-1.5f, 1f, -4.8f));
        firstName.setString("WAITING");

        Color3f red = new Color3f(1.0f, 0f, 0f);
        Color3f reddish = new Color3f(1.0f, 0.1f, 0.1f);
        Appearance appearance = new Appearance();
        Material material = new Material(reddish, reddish, reddish, red, 82.0f);
        material.setLightingEnable(true);
        appearance.setMaterial(material);

        Shape3D shape3D1 = new Shape3D();
        shape3D1.setGeometry(firstName);
        shape3D1.setAppearance(appearance);

        TransformGroup transformGroup = new TransformGroup();
        Transform3D transform3D = new Transform3D();
        Vector3f v3f = new Vector3f(-1.0f, -1.0f, -4f);
        transform3D.setTranslation(v3f);
        transformGroup.setTransform(transform3D);
        transformGroup.addChild(shape3D1);
        branchGroup.addChild(transformGroup);
    }
    
    private void addGuessingText(BranchGroup branchGroup) {
        Font3D font3D = new Font3D(new Font("Arial", Font.BOLD, 1),
                new FontExtrusion());

        String text1 = "THE OTHER";
        String text2 = "PLAYER SAYS";
        String text3 = "THEY HAVE A";
        String text4 = "SCORE OF "+bluffValue;
        
        Text3D mod1 = new Text3D(font3D, text1, new Point3f(-1.5f, 1.5f, -4.8f));
        mod1.setString(text1);
        Text3D mod2 = new Text3D(font3D, text2, new Point3f(-1.5f, 0.5f, -4.8f));
        mod2.setString(text2);
        Text3D mod3 = new Text3D(font3D, text3, new Point3f(-1.5f, -0.5f, -4.8f));
        mod3.setString(text3);
        Text3D mod4 = new Text3D(font3D, text4, new Point3f(-1.5f, -1.5f, -4.8f));
        mod4.setString(text4);

        Color3f blue = new Color3f(0f, 0.8f, 0.8f);
        Color3f blueish = new Color3f(0.1f, 0.5f, 0.5f);
        Appearance appearance = new Appearance();
        Material material = new Material(blueish, blueish, blueish, blue, 82.0f);
        material.setLightingEnable(true);
        appearance.setMaterial(material);

        Shape3D shape3D1 = new Shape3D();
        shape3D1.setGeometry(mod1);
        shape3D1.setAppearance(appearance);
        Shape3D shape3D2 = new Shape3D();
        shape3D2.setGeometry(mod2);
        shape3D2.setAppearance(appearance);
        Shape3D shape3D3 = new Shape3D();
        shape3D3.setGeometry(mod3);
        shape3D3.setAppearance(appearance);
        Shape3D shape3D4 = new Shape3D();
        shape3D4.setGeometry(mod4);
        shape3D4.setAppearance(appearance);

        TransformGroup transformGroup = new TransformGroup();
        Transform3D transform3D = new Transform3D();
        Vector3f v3f = new Vector3f(-2.0f, 0.0f, -4f);
        transform3D.setTranslation(v3f);
        transformGroup.setTransform(transform3D);
        transformGroup.addChild(shape3D1);
        transformGroup.addChild(shape3D2);
        transformGroup.addChild(shape3D3);
        transformGroup.addChild(shape3D4);
        branchGroup.addChild(transformGroup);
    }

    public void rollDice() {
    	die1 = (int)(Math.round(Math.random()*5 + 1));
    	die2 = (int)(Math.round(Math.random()*5 + 1));
    }
    
    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getLives() {
        return lives;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }
}
