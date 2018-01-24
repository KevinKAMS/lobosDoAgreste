//
//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

import java.lang.Math;
import java.util.regex.*;

class Brain extends Thread implements SensorInput
{
    //---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to agent
    // - starts thread for this object
    public Brain(SendCommand agent, 
		 String team, 
		 char side, 
		 int number, 
		 String playMode)
    {
	m_timeOver = false;
	m_agent = agent;
	m_memory = new Memory();
	m_team = team;
	m_side = side;
	m_number = number;
	m_playMode = playMode;
	m_waiting=true;
	start();
    }


    //---------------------------------------------------------------------------
    // This is main brain function used to make decision
    // In each cycle we decide which command to issue based on
    // current situation. the rules are:
    //
    //	1. If you don't know where is ball then turn right and wait for new info
    //
    //	2. If ball is too far to kick it then
    //		2.1. If we are directed towards the ball then go to the ball
    //		2.2. else turn to the ball
    //
    //	3. If we dont know where is opponent goal then turn wait 
    //				and wait for new info
    //
    //	4. Kick ball
    //
    //	To ensure that we don't send commands to often after each cycle
    //	we waits one simulator steps. (This of course should be done better)

    // ***************  Improvements ******************
    // Allways know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    public void run(){
    	moveToMySide();
		while( !m_timeOver){
			if(m_memory.isEmpty()){

			}
			else if (playOn) {
				ball = m_memory.getObject("ball");
				player = m_memory.getObject("player " + m_team);
				flag = m_memory.getObject("flag c");
				if (m_side == 'l')
					flagTop = m_memory.getObject("flag p r t");	
				else
					flagTop = m_memory.getObject("flag p l t");
				if (m_side == 'l')
					flagBot = m_memory.getObject("flag p r b");	
				else
					flagBot = m_memory.getObject("flag p l b");
				if( m_side == 'l' )
					goal = m_memory.getObject("goal r");
				else
					goal = m_memory.getObject("goal l");

				
				if(!posicao)
				{
					if (flag == null)
					{
						m_agent.turn(44);
						m_memory.clearInfo();
					}
					else if(flag.m_distance > 2){
					    if( flag.m_direction < -2.5 || flag.m_direction > 2.5 ){
							m_agent.turn(flag.m_direction);
							m_memory.clearInfo();
					    }
					    else{ 
						float power=dash_factor*(flag.m_distance);
						dash_factor*=0.9; //this decreases the dash factor, it is restored when we get new visual info
						if(power > 100)
						    power=100;
						m_agent.dash(power);
					    }
					} else {
						System.out.println("Eu sou o Meia e cheguei no meu destino");
						posicao = true;
					}
				}
				else
				{
					System.out.println("vou rodar minha rotina");
					chutei = 0;
					if(ball==null){
			    		System.out.println("2");
						m_agent.turn(44);
						m_memory.clearInfo();
					}
					else if(ball!=null && ball.m_distance > 5 && (player != null && player.m_distance < ball.m_distance)){
						System.out.println("3");
						m_agent.turn(ball.m_direction);
					    m_memory.clearInfo();
					}
					else if(ball != null && ball.m_distance > 0.7 && ball.m_distance <= 20)
					{
						if( ball.m_direction < -2.5 || ball.m_direction > 2.5 ){
						System.out.println("4");
						m_agent.turn(ball.m_direction);
						m_memory.clearInfo();
					    }
					    else{ //otherwise run toward the ball
					    System.out.println("5");
						float power=dash_factor*ball.m_distance;
						dash_factor*=0.9; //this decreases the dash factor, it is restored when we get new visual info
						if(power > 100)
						    power=100;
						m_agent.dash(power);
					    }
					}
					else if(ball != null && ball.m_distance <= 1.0)
					{
						if(goal != null && goal.m_distance < 30)
						{
							System.out.println("6");
							m_agent.kick(goal.m_distance*4, goal.m_direction);
						}
					    else if( player == null )
						{
							System.out.println("7");
						    m_agent.turn(44);
						    m_memory.clearInfo(); //it was waitForNewInfo(), but I don't think it needs to explicitly wait --Edgar
						}
					    else if(player != null && chutei < 1)
					    {
					    	System.out.println("8");
					    	m_agent.kick(player.m_distance + 40, player.m_direction);
					    	chutei++;
					    }
					    else
					    {
					    	System.out.println("9");
					    	posicao = false;
					    }
					}
					else
					{
						System.out.println("10");
						posicao = false;
					}
				}
			}
			else {
				moveToMySide();
			}




			 		try{
			 		    Thread.sleep(2*SoccerParams.simulator_step);
			 		}catch(Exception e){}
			m_waiting=true;
	    }
		m_agent.bye();
    }


    //===========================================================================
    // Here are suporting functions for implement logic

    //===========================================================================
    // Implementation of SensorInput Interface

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info)
    {
	m_memory.store(info);
	dash_factor=20;
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message)
    {
    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message)
    {						 
		String[] messages=message.split("_");


    	if(messages[0].compareTo("goal")==0) playOn=false;

    	if(messages[0].compareTo("kick")==0 || message.compareTo("play_on")==0) playOn=true;

		if(message.compareTo("time_over")==0) m_timeOver=true;
	
		if(Pattern.matches("^before_kick_off.*", message)){
			m_playMode=message;

			playOn=false;
		}


    }

    //--------------------------------------------------------------------------
    // This function receives a sense_body message
    // It doesn't do anything other than signaling the time to send a command
    public void sense(String message)
    {
		m_waiting=false;
    }

    public void moveToMySide()
    {
		m_agent.move(-0.3*52.5, 34-0.5*68.0);
    }

    //===========================================================================
    // Private members
    private boolean posicao = false;
    private boolean playOn = false;
    private int chutei = 0;
    private ObjectInfo ball, line, goalR, goalL, goal, object, flag, player, flagTop, flagBot;
    private int m_number;
    private SendCommand	m_agent;			// robot which is controled by this brain
    private Memory	m_memory;				// place where all information is stored
    private char	m_side;
    volatile private boolean	m_timeOver,m_waiting;
    private String  m_team;
    volatile private String  m_playMode;
    volatile private float   dash_factor=20; //
}
