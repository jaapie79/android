/*
 * This file is part of the dSploit.
 *
 * Copyleft of Simone Margaritelli aka evilsocket <evilsocket@gmail.com>
 *
 * dSploit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dSploit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dSploit.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.evilsocket.dsploit.plugins.mitm;

import it.evilsocket.dsploit.core.System;
import it.evilsocket.dsploit.net.Target;
import it.evilsocket.dsploit.tools.Ettercap.OnAccountListener;
import it.evilsocket.dsploit.tools.Ettercap.OnReadyListener;

public class SpoofSession 
{
	private static final String TAG = "SPOOFSESSION";
	
	private boolean mWithProxy  	= false;
	private boolean mWithServer 	= false;
	private String  mServerFileName = null;
	private String  mServerMimeType = null;
	
	public static abstract interface OnSessionReadyListener 
	{
		public void onSessionReady();
	}
	
	public SpoofSession( boolean withProxy, boolean withServer, String serverFileName, String serverMimeType ) {
		mWithProxy  	= withProxy;
		mWithServer 	= withServer;
		mServerFileName = serverFileName;
		mServerMimeType = serverMimeType;
	}
	
	public SpoofSession() {
		// Standard spoof session with only transparent proxy
		this( true, false, null, null );
	}
			
	public void start( Target target, final OnSessionReadyListener listener ) {
		this.stop();
		
		if( mWithProxy )
			new Thread( System.getProxy() ).start();
				
		if( mWithServer )
		{
			try
			{
				System.getServer().setResource( mServerFileName, mServerMimeType );
				new Thread( System.getServer() ).start();
			}
			catch( Exception e )
			{
				System.errorLogging( TAG, e );
				mWithServer = false;
			}
		}
						
		System.getEttercap().spoof( target, new OnReadyListener(){
			@Override
			public void onReady() 
			{				
				// ip forwarding has to be enabled here, cause ettercap seems
				// to disable it on startup
				System.setForwarding( true );
				
				if( mWithProxy )
					System.getIPTables().portRedirect( 80, System.HTTP_PROXY_PORT );
				
				listener.onSessionReady();
			}
			
		}).start();		
	}
	
	public void startWithPasswords( Target target, OnAccountListener listener ) {
		this.stop();						
		System.getEttercap().spoofPasswords( target, listener ).start();		
	}
	
	public void start( final OnSessionReadyListener listener ) {
		this.start( System.getCurrentTarget(), listener );
	}
	
	public void startWithPasswords( OnAccountListener listener ) {
		this.startWithPasswords( System.getCurrentTarget(), listener );
	}
	
	public void stop() {
		System.getEttercap().kill();
		System.setForwarding( false );
				
		if( mWithProxy )
		{
			System.getIPTables().undoPortRedirect( 80, System.HTTP_PROXY_PORT );
			System.getProxy().stop();
			System.getProxy().setRedirection( null, 0 );
			System.getProxy().setFilter( null );
		}
		
		if( mWithServer )		
			System.getServer().stop();		
	}
}
