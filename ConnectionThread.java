package finalproject.Mia;

import com.fasterxml.jackson.databind.ObjectMapper;
import finalproject.Mia.model.PlayerData;

import java.io.*;
import java.net.Socket;

public class ConnectionThread extends Thread
{
    protected Socket socket;
    protected String json;
    protected ObjectMapper objectMapper;
    protected PlayerData playerData;

    public ConnectionThread(Socket clientSocket)
    {
        this.socket = clientSocket;
    }

    public void run()
    {
        try
        {
            //read data from client
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str = bufferedReader.readLine();
            json = str;
            objectMapper = new ObjectMapper();
            playerData = objectMapper.readValue(json, PlayerData.class);
            System.out.println("Received:\t" + playerData.toString());

            //perform business logic
            String dataToSend = decideNextScreen(playerData.getScreen());

            //send data to the client
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            printStream.println(dataToSend);
            String empty = null;
            printStream.println(empty);
            printStream.close();
        }
        catch(IOException e)
        {

        }
    }

    private String decideNextScreen(String currentScreen)
    {
        String nextScreen = "";

        //if player2 is coming from title screen OR it is not the players turn, show waiting screen
        if(currentScreen.equalsIgnoreCase("title") && playerData.getPlayerNumber()!= 1
                || playerData.getTurn() == 0)
        {
            nextScreen = "waiting";
            playerData.setScreen(nextScreen);
        }
        else if(currentScreen.equalsIgnoreCase("title") && playerData.getPlayerNumber() == 1)
        {
            nextScreen = "play";
            playerData.setScreen(nextScreen);
        }
        System.out.println("Deciding screen...\t" + playerData.toString());
        return playerData.toString();
    }
}