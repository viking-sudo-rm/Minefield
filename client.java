import processing.core.*; import controlP5.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; import javax.sound.midi.*; import javax.sound.midi.spi.*; import javax.sound.sampled.*; import javax.sound.sampled.spi.*; import java.util.regex.*; import javax.xml.parsers.*; import javax.xml.transform.*; import javax.xml.transform.dom.*; import javax.xml.transform.sax.*; import javax.xml.transform.stream.*; import org.xml.sax.*; import org.xml.sax.ext.*; import org.xml.sax.helpers.*; public class Minefield2D extends PApplet {/*

Minefield 2D Multiplayer
Jack Seibert, Jasen Chan, Will Merrill

Controls:
W, A, S, D to move
Click to drop a mine

Requirements:
ControlP5 module installed in correct location
Folder with necessary game fonts

Using the Login Screen:
To enter text in a login box, click into the box, write the string,
hit enter (important!), then (after you have entered all the fields
in the form), hit the 'JOINGAME' button to submit your results.

Definitions of Fields in Screen:
'IPADDRESS' -- the IP address of the server to which you are connecting
(if the server is running on the same machine as the client, use 'localhost').
'USERNAME' -- the name that you will be identified as on the server.
Note that if you die under one name, you will stay dead under that
name on the server until the server is rebooted.

*/

//variables

 //module for input boxes and buttons
ControlP5 controlP5; //control P5 instance
ControlWindow controlWindow; //login window
String username; //username
String ip; //IP address of server
String logoruser = null; //name on server (null if not logged in)
Textfield chat;
Textarea chatText;
String lastresponse;
ArrayList playas = new ArrayList(); //players logged in on the server
ArrayList mines = new ArrayList(); //mines placed on the server
float realx; //x position of player
float realy; //y position of player
float realz; //z position of player
int[] intro_x, intro_y, intro_wx, intro_wy; //variables for intro animation
int[] end_x, end_y; //variables for death animation
PFont font; //variable for font
int mover; //variable for death animation
boolean end_dead[]; //variable for death animation
int end_rotation[]; //variable for death animation
int end_size[]; //variable for death animation

class Player { //object to store players returned from the server
  String name;
  float x; 
  float y; 
  float z;
  Player(String iname,float ix,float iy,float iz) {
    name = iname;
    x = ix;
    y = iy;
    z = iz;
  }
}

class Mine { //object to store mines returned from the server
  float x;
  float z;
  Mine(float ix, float iz) {
    x = ix;
    z = iz;
  }
}

public void activateChat() { //chat function.. currently disabled
  chat.setFocus(true);
}

public void initializeChat() { //chat function.. currently disabled
  chat = controlP5.addTextfield("chatInput", 10, 780, 580, 20);
}

public void chatInput(String theText) { //chat function.. currently disabled
  geturl("http://" + ip + ":4000/comment|" + logoruser + "|" + theText);
}

public void getChat() { //chat function.. currently disabled
  geturl("http://" + ip + ":4000/renderdata|comments|" + logoruser);
  if (lastresponse != null) {
    int chatHeight = split(lastresponse, "\n").length * 20;
    if (chatHeight > 100) {
      chatHeight = 100;
    }
    chatText = controlP5.addTextarea("chatText", lastresponse, 10, 780 - chatHeight, 580, chatHeight);
    chatText.setColorBackground(color(0,69,103));
  }
}

public void parsePlayers(String input) { //parse a string of player info returned from server
  if (input != null) {
    String l[];
    String name;
    String coords[];
    float x;
    float y;
    float z;
    String players[] = input.split(";");
    playas.clear();
    for (int i = 0; i < players.length - 1; i++) {
      println(players[i]);
      l = players[i].split(":");
      println("l[0]: " + l[0]);
      //println("l[1]: " + l[1]);
      name = l[0];
      try {
        coords = l[1].split(",");
        x = Float.valueOf(coords[0]).floatValue();
        y = Float.valueOf(coords[1]).floatValue();
        z = Float.valueOf(coords[2]).floatValue();
        println(name);
        println(x);
        println(y);
        println(z);
        if (name == logoruser) {
          realx = x;
          realy = y;
          realz = z;
        }
        playas.add(new Player(name,x,y,z));
      } catch (ArrayIndexOutOfBoundsException e) {
        println("Error: " + e);
      }
    }
  }
}

public void parseMines(String input) { //parse a string of mine info returned from the server
  if (input != null && input.indexOf(":") == -1) {
    String[] minesGet = input.split(";");
    mines.clear();
    for (int i = 0; i < minesGet.length; i++) {
      String[] coords = minesGet[i].split(",");
      try {
        mines.add(new Mine(Float.valueOf(coords[0]).floatValue(), Float.valueOf(coords[2]).floatValue()));
      } catch (NumberFormatException e) {
        println("Error: " + e);
      }
    }
  }
}

boolean firstTimeDead = true;

public void setup() { //pretty standard as far as setup()s go
  frameRate(30); //a lower frame rate means less requests to the server (so it isn't overloaded)
  size(800,600);
  smooth();
  rectMode(CENTER);
  
  font = loadFont("Futura-CondensedMedium-48.vlw"); //load a font located in the data folder
  textFont(font);
  
  intro_x = new int[25];
  intro_y = new int[25];
  intro_wx = new int[25];
  intro_wy = new int[25];
  for (int i = 0; i < intro_x.length; i++) {
    intro_x[i] = (int)random(50,750);
    intro_y[i] = (int)random(50,550);
    intro_wx[i] = (int)random(0,2);
    if (intro_wx[i] == 0)
      intro_wx[i] = -1;
    intro_wy[i] = (int)random(0,2);
    if (intro_wy[i] == 0)
      intro_wy[i] = -1;
  }
  
  end_x = new int[11];
  end_y = new int[11];
  end_dead = new boolean[11];
  end_rotation = new int[11];
  end_size = new int[11];
  for (int i = 0; i < end_x.length; i++) {
    end_x[i] = 25;
    end_y[i] = (i + 1) * 50;
    end_dead[i] = false;
    end_rotation[i] = 0;
    end_size[i] = 1;
  }
  mover = 0;

  //set up ControlP5
  controlP5 = new ControlP5(this);
  //controlP5.setAutoDraw(false);

  //the login window:
  controlWindow = controlP5.addControlWindow("loginWindow",140,20,130,140);
  controlWindow.hideCoordinates();
  controlWindow.setBackground(color(95,95,95));
  controlWindow.setTitle("Login");

  //IP input textarea:
  Textfield ipText = controlP5.addTextfield("IPAddress",10,10,100,20);
  ipText.setWindow(controlWindow);

  //username input textarea:
  Textfield usernameText = controlP5.addTextfield("username",10,60,100,20);
  usernameText.setWindow(controlWindow);

  //login button:
  controlP5.Button joingameButton = controlP5.addButton("joingame",10,10,110,100,20);
  joingameButton.setWindow(controlWindow);
}

public void tank(float tx, float tz) { //function to draw a tank/player
    rect(tx, tz, 20,30);
    rect(tx, tz  -15, 5, 30);
    ellipse(tx, tz, 15, 15);
}

public void draw() { //execute this in an infinite loop
  background(255);
  if (gameStarted()) { //if the game is on, let the player play
    getResponse("http://" + ip + ":4000/renderdata|mines"); //positions of mines from server
    parseMines(lastresponse);
    renderMines();
    getResponse("http://" + ip + ":4000/renderdata|positions"); //position of players from server
    parsePlayers(lastresponse);
    renderPlayers();
    //chat commented out to reduce lag on network "student"
    /*initializeChat();
    getChat();
    getResponse("http://" + ip + ":4000/renderdata|cooldown|" + logoruser);
    renderCooldownMeter(lastresponse);*/
    
    if (keyPressed) {
      //controls "WASD"
      if (key == 'w') {
        geturl("http://" + ip + ":4000/up|" + logoruser);
      }
      else if (key == 'a') {
        geturl("http://" + ip + ":4000/left|" + logoruser);
      }
      else if (key == 'd') {
        geturl("http://" + ip + ":4000/right|" + logoruser);
      }
      else if (key == 's') {
        geturl("http://" + ip + ":4000/down|" + logoruser);
      }
      else if (key == 10) {
        activateChat();
      }
    }
    if (mousePressed) {
      addMine(logoruser);
    }
  }
  else if (isDead(logoruser)) { //if the player is dead, show death animation
    logoruser = "dead&%##!!;";
    loadFont("Futura-CondensedMedium-48.vlw");
    for (int i = 0; i < end_x.length; i++) {
      if (!end_dead[i]) {
        stroke(255,0,0);
        fill(255,0,0);
        ellipse(730, end_y[i], 10, 10);
        stroke(255,239,54);
        fill(0,255,0);
        tank(end_x[i], end_y[i]);
        if (end_x[i] < 730) {
          if (i <= mover / 6) {
            end_x[i] += 5;
          }
        }
        else {
          end_dead[i] = true;
        }
      }
      else {
        if (end_size[i] <= 10) {
          pushMatrix();
          translate(730, end_y[i]);
          rotate(radians(end_rotation[i]));
          stroke(255,239,54);
          fill(255,0,0);
          rect(0, 0, 30 * end_size[i], 30 * end_size[i]);
          end_rotation[i] += 20;
          end_size[i] += 1;
          popMatrix();
        }
        else if (i >= 10) {
          for (int w = 0; i < end_x.length; i++) {
            end_x[w] = 25;
            end_y[w] = (i + 1) * 50;
            end_dead[w] = false;
            end_rotation[w] = 0;
            end_size[w] = 1;
          }
        }
      }
    }
    mover++;
    fill(255,0,0);
    text("GAME OVER", width/2, height/2);
  }
  else { //if the player hasn't logged in, show the login animation
    fill(133,133,133);
    text("MINEFIELD", width/1.45f, height/2);
    fill(255,0,0);
    stroke(0,255,0);
    for (int i = 0; i < intro_x.length; i++) {
      rect(intro_x[i],intro_y[i],30,30);
      intro_x[i] += intro_wx[i] * random(5,10);
      intro_y[i] += intro_wy[i] * random(5,10);
      if (intro_x[i] >= 800 || intro_x[i] <= 0) {
         intro_wx[i] = -intro_wx[i];
      }
      else if (intro_y[i] >= 600 || intro_y[i] <= 0) {
         intro_wy[i] = -intro_wy[i];
      }
    }
  }
}

public void stop() { //when the "X" quit button is clicked, log the player out
  logout();
  super.stop();
}

public void addMine(String player) { //function to add a mine
  getResponse("http://" + ip + ":4000/mine|" + player); //send request to server
}

public void renderPlayers() { //function to render the players logged in
  float thisx;
  float thisy;
  float thisz;
  Player thing;
  for(int x = 0; x < playas.size(); x++)
  {
    thing = (Player)playas.get(x);
    thisx = thing.x;
    thisy = thing.y;
    thisz = thing.z;
    stroke(255,239,54);
    if (thing.name.equals(logoruser)) {
      fill(0,255,0);
      tank(thisx + width / 2, -thisz + height / 2);
    }
    else {
      fill(0,0,255);
      tank(thisx + width / 2, -thisz + height / 2);
    }
    PFont newFont = loadFont("Futura-CondensedMedium-12.vlw");
    textFont(newFont);
    text(thing.name, thisx + width / 2 - 15, -thisz + height / 2 - 18);
  }
  print("\n\n");
}

public void renderMines() { //function to render the mines
  Mine mine;
  stroke(255,0,0);
  fill(255,0,0);
  for (int i = 0; i < mines.size(); i++) {
    mine = (Mine) mines.get(i);
    ellipse(mine.x + (width / 2),height - (mine.z + (height / 2)),10,10);
  }
}

public void renderCooldownMeter(String input) { //taken out to reduce lag
  println("DEATH TO COOLDOWN: " + input);
  if (input != null && input != "" && input.indexOf(",") == -1) {
    float opacity = Float.valueOf(input).floatValue() / 10;
    opacity -= 50;
    opacity *= -1;
    opacity += 50;
    println("OPACITY: " + opacity);
    fill(180, 22, 22, opacity);
    ellipse(width / 2, 50, 100, 100);
  }
}

public boolean gameStarted() { //returns whether or not the game is on
  return (logoruser != null && !isDead(logoruser));
}

public void joingame(int theValue) { //called when the login form is submitted
  do { //try to login until a welcome message is received
    login(username);
  } while (file("http://" + ip + ":4000/welcome.gameinfo") != "hello world");
  controlWindow.hide();
}

public void IPAddress(String theValue) { //get the ip from the form
  ip = theValue;
  print(ip);
}

public void username(String theValue) { //get the username from the form
  username = theValue;
  print(username);
}

public void logout() { //logout from a server
  if (logoruser != null) {
    getResponse("http://" + ip + ":4000/logout|" + logoruser);
    logoruser = null;
  }
}

public void login(String user) { //login to a server
  getResponse("http://" + ip + ":4000/login|" + user);
  /*String worked = lastresponse;
   println("\n\n\n" + worked);
   if (worked.equals("1")) {
   logoruser = user;
   return true;
   }
   else {
   return false;
   }*/
  logoruser = user;
}

public String getResponse(String path) { //get a response from the server
  try {
    URL url = new URL(path);
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    String str;

    lastresponse = "";

    while ((str = in.readLine()) != null) {
      lastresponse += str + "\n";
    }

    in.close();
    return lastresponse;
  }
  catch (MalformedURLException e) {
  }
  catch (IOException e) {
  }
  return "";
}

public boolean isDead(String name) { //test if a player is dead
  if (logoruser == "dead&%##!!;") {
    return true;
  }
  if (logoruser != null) {
    getResponse("http://" + ip + ":4000/renderdata|dead");
    try {
      String[] names = lastresponse.split(";");
      for (int i = 0; i < names.length; i++) {
        if (names[i].equals(name)) {
          return true;
        }
      }
    } catch (NullPointerException e) {
      println("Error: " + e);
    }
    return false;
  }
  return false;
}

public void geturl(String path) { //send a reguest to the server without getting a response
  print(path);
  try {
    URL url = new URL(path);
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    String string;
    String response = "";

    while (!(string = in.readLine()).equals(null)) {
      response += string + "\n";
    }

    in.close();
  }
  catch (MalformedURLException e) {
    println("Error:" + e);
  }
  catch (IOException e) {
    println("Error:" + e);
  }
}

public String file(String path) { //same as getResponse(), but doesn't return newline characters "\n"
  print(path);
  String response = "";
  try {
    URL url = new URL(path);
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    String string;

    while (!(string = in.readLine()).equals(null)) {
      response += string;
    }

    in.close();
  }
  catch (MalformedURLException e) {
    println("Error:" + e);
  }
  catch (IOException e) {
    println("Error:" + e);
  }
  
  return response;
}


  static public void main(String args[]) {     PApplet.main(new String[] { "Minefield2D" });  }}