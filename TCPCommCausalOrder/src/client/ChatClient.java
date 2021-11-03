package client;

import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import protocol.Message;
import protocol.MessageType;

public class ChatClient extends JFrame {

    // Chat client components
    private Client client;
    private String clientName, hostname;
    private int portNumber;

    // GUI Components
    private final int WIDTH = 700;
    private final int HEIGHT = 400;
    private JTabbedPane roomsPane;
    private Map<Integer,RoomPanel> rooms;
    private JTextField messageToSend;
    private JTextField targetToSend;
    private JLabel jLabel;
    private JLabel jLabelMsg;
    private JButton send, cancel;
    private int counter = 0; 
    private JList<String> userList;
    
    /**
     * Creates a new chat client which will connect to the specified server.
     *
     * settings ClientSettings object from which to draw connection settings
     */
    public ChatClient(String clientName, String hostname, int portNumber) {

        this.clientName = clientName;
        this.hostname = hostname;
        this.portNumber = portNumber;

        initFrame();
        initComponents();

        this.client = new Client(clientName, hostname, portNumber);

        // Login handlers
        this.client.registerHandler(MessageType.LOGIN_SUCCESS, this::displayWelcome);
        this.client.registerHandler(MessageType.LOGIN_FAILURE, this::displayRetryDialog);

        // Communication message handlers
        this.client.registerHandler(MessageType.CHAT, this::displayMessage);

        // Command reply messages
        this.client.registerHandler(MessageType.JOIN_ROOM_SUCCESS, this::joinRoom);
        this.client.registerHandler(MessageType.JOIN_ROOM_FAILURE, this::joinRoomFailure);
        this.client.registerHandler(MessageType.LEAVE_ROOM_SUCCESS, this::leaveRoom);
        this.client.registerHandler(MessageType.LEAVE_ROOM_FAILURE, this::leaveRoomFailure);

        this.client.establishConnection();
    }

    /**
     * Initializes all of the components present in the GUI. This includes buttons, text boxes,
     * action listeners, etc.
     */
    private void initComponents() {
        rooms = new HashMap<>();
        RoomPanel globalRoom = new RoomPanel(0, (int)(WIDTH * .95), (int)(HEIGHT * .7));
        rooms.put(0, globalRoom);

        roomsPane = new JTabbedPane(SwingConstants.TOP);
        roomsPane.addTab("Global room", globalRoom);
        add(roomsPane);

        jLabel = new JLabel("Target Client");
        add(jLabel);
        
        targetToSend = new JTextField(15);
        //messageToSend.addActionListener(ae -> sendStringMessage());
        add(targetToSend);
              
        jLabelMsg = new JLabel("Msg");
        add(jLabelMsg);
        
        messageToSend = new JTextField(15);
        messageToSend.addActionListener(ae -> sendStringMessage());
        add(messageToSend);
        
        send = new JButton("Send");
        send.addActionListener(ae -> sendStringMessage());
        add(send);

        cancel = new JButton("Cancel");
        cancel.addActionListener(ae -> cancelMessage());
        add(cancel);
       
    }

    /**
     * Initializes frame settings. This includes things like the size of the frame, layout managers,
     * and the like.
     */
    private void initFrame() {
        // Frame settings
        this.setSize(WIDTH, HEIGHT);
        this.setLayout(new FlowLayout());
        this.setResizable(false);
        this.setTitle("Chat Client - " + clientName);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());
    }

    /**
     * Determines which room you are currently sending messages to.
     *
     * @return The ID for the room that messages are currently being sent to.
     */
    private int getCurrentRoom() {
        RoomPanel room = (RoomPanel) roomsPane.getSelectedComponent();
        for (Map.Entry<Integer, RoomPanel> roomEntry : rooms.entrySet() ) {
            if ( roomEntry.getValue().equals(room) ) {
                return roomEntry.getKey();
            }
        }
        return 0;
    }

    private void appendToRoom(String message, int roomId) {
        RoomPanel room = this.rooms.get(roomId);
        SwingUtilities.invokeLater(() -> room.append(message));
    }

    
    private void cancelMessage() {
    	
    	messageToSend.setText("");
    	this.counter = counter +1;
        String localEvent = "Local Event :Cancel" +lamportLocalTime(counter) +"\n" +"########" +"\n";
        System.out.println(" Lambort Time at Cancel :"+ localEvent);
     	appendToRoom(localEvent, getCurrentRoom());
    }
    
    
    /**
     * Sends a standard CHAT message containing the text present in the text field.
     */
    private void sendStringMessage() {
        String message = this.messageToSend.getText();
        String targetClientName = this.targetToSend.getText();
        //String targetClientName = userList.getSelectedValue();
        System.out.println("Selected targetClientName :" +targetClientName);
        int currentRoom = getCurrentRoom();
        this.counter = counter +1;
        String sendCounter = "Local : Send" +lamportLocalTime(counter) +"\n" +"=============" +"\n";
                
        if( ! message.trim().isEmpty() ) {
                Message<String> m = new Message<>(clientName, currentRoom, message, MessageType.CHAT);
                m.setCounter(counter);
                m.setTargetClientName(targetClientName);
                
                this.client.writeMessage(m);
                String toDisplay = String.format("%s: %s\n", m.getSender(), m.getContents());
            	appendToRoom(toDisplay, getCurrentRoom());
        }
        
       
    	System.out.println(" Lambort Time at Send :"+ sendCounter);
    	appendToRoom(sendCounter, getCurrentRoom());
        this.messageToSend.setText("");
    }

    /**
     * This handler is for simple chat messages and will append the contents of the message to the
     * text area for this room.
     *
     * @param message A message, presumably of type CHAT, which contains the object which will be
     * converted to a string and displayed in the message history text area.
     */
    private <E extends Serializable> void displayMessage(Message<E> message) {
    	
    	String toDisplay = "";
    	System.out.println(" clientName: "+clientName +" Sender :"+ message.getSender());
    	System.out.println(" Sender Id: "+message.getSenderId() +" Destination :"+ message.getDestination());
    	if(this.clientName.equals(message.getSender()))
    	{
	        //toDisplay = String.format("%s: %s\n", message.getSender(), message.getContents());
    		System.out.println("Self Receiving... Ignore the message");
    	}
    	else
    	{
	    	int maxTime = calcRecvTimestamp(message.getCounter(), this.counter);
	    	System.out.println("Counter :" + message.getCounter() + "counter :" + counter +" maxTime :" +maxTime);
	    	String lamportLocalTime = lamportLocalTime(maxTime);
	    	System.out.println(" ### Receiving Message ..." + lamportLocalTime);
	        toDisplay = String.format("%s: %s \n", message.getSender(), message.getContents());
	        
	        System.out.printf("Printing message to room %s\n", message.getDestination());
	        appendToRoom(toDisplay, message.getDestination());
	        
	        String recvLambortClock = String.format("Recv : %s: %s \n **********\n", message.getSender(), lamportLocalTime);
	        appendToRoom(recvLambortClock, message.getDestination());
    	}
       
    }
    
	private String lamportLocalTime(int counter) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		String time = formatter.format(date);
		String localTime = "(LAMPORT_TIME= " + counter + " LOCAL_TIME=" + time + ")";

		return localTime;
	}

	private int calcRecvTimestamp(int recv_time_stamp, int counter) {
		int recvEventClockTime = Math.max(recv_time_stamp, counter) + 1;
		this.counter = recvEventClockTime;
		return recvEventClockTime;
	}
    
    private <E extends Serializable> void displayWelcome(Message<E> message) {
        String toDisplay = String.format("You have connected to %s:%d!\n", hostname, portNumber);
        appendToRoom(toDisplay, message.getDestination());
    }

    private <E extends Serializable> void joinRoomFailure(Message<E> message) {
        String str = message.getContents().toString();
        SwingUtilities.invokeLater(() -> rooms.get(getCurrentRoom()).append(str));
    }

    private <E extends Serializable> void displayRetryDialog(Message<E> message) {
        String newUserId = JOptionPane.showInputDialog(this, message.getContents(), "Enter Username", JOptionPane.PLAIN_MESSAGE);
        Message<String> newLogin = new Message<>(newUserId, Message.SERVER_ID, newUserId, MessageType.LOGIN_INFORMATION);
        message.setSenderId(this.client.getClientId());
        client.writeMessage(newLogin);
    }

    private <E extends Serializable> void joinRoom(Message<E> message) {
        RoomPanel newRoom = rooms.get(message.getDestination());

        // Only create the room entry if it doesn't already exist
        if( newRoom == null ) {
            newRoom = new RoomPanel(message.getDestination(), (int)(WIDTH * .95), (int)(HEIGHT * .7));
            rooms.put(message.getDestination(), newRoom);
            roomsPane.addTab(message.getContents().toString(), newRoom);
        }

        roomsPane.setSelectedComponent(rooms.get(message.getDestination()));

        String toDisplay = String.format("Welcome to room %s\n", message.getContents());
        appendToRoom(toDisplay, message.getDestination());
    }

    public <E extends Serializable> void leaveRoom(Message<E> message) {
        if ( message.getContents() instanceof Integer ) {
            RoomPanel rp = rooms.remove(message.getContents());
            roomsPane.remove(rp);
        }
    }

    public <E extends Serializable> void leaveRoomFailure(Message<E> message) {
        appendToRoom(String.format("%s: %s", message.getSender(), message.getContents()), getCurrentRoom());
    }
    
    /**
     * This is dispatcher which will handle sending audio events for audio messages.
     */
    private class KeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            // Ignore the event if it was fired by the message field
            if( e.getSource() == messageToSend ) {
                return false;
            }

            return false;
        }
    }
    
    public static void main( String[] args ) {

        if( args.length > 2 ) {
        	ChatClient cc = new ChatClient(args[0], args[1],Integer.parseInt(args[2]));
        	// Create the chat client
            SwingUtilities.invokeLater(() -> cc.setVisible(true));
        } else {
        	System.out.println(" Invalid Arguments...");
        }

    }
}
