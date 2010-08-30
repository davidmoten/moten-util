/*
 * Created on May 6, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
Copyright 2003 Joseph Barnett

This File is part of "one 2 oh my god"

"one 2 oh my god" is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
Free Software Foundation; either version 2 of the License, or
your option) any later version.

"one 2 oh my god" is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with "one 2 oh my god"; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 */
package itunes.client.request;
import itunes.FieldPair;
import org.cdavies.itunes.*;
/**
 * @author jbarnett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ServerInfoRequest extends Request {
	
	public ServerInfoRequest(String server, int port, ConnectionStatus status) throws NoServerPermissionException {
		super(server,port, "server-info",status);
	}
	public double getServerVersion() {
		
		
		int index = fieldPairs.indexOf(new FieldPair("apro",new byte[0],0,0));
		if (index == -1)
			return index;
		FieldPair fp = (FieldPair)fieldPairs.get(index);
		
		
		return readInt(fp.value,0,2) + (0.01*readInt(fp.value,2,2));
	}
	public String getServerName() {	
		
		int index = fieldPairs.indexOf(new FieldPair("minm",new byte[0],0,0));
		if (index == -1)
			return "";
		FieldPair fp = (FieldPair)fieldPairs.get(index);
		
		System.out.println(new String(fp.value));
		
		for(int i=0; i<fp.value.length;i++){
			System.out.println("byte " + i + " " + fp.value[i]);
		}
		
		try{
			return new String(fp.value, "UTF-8");
		}catch(Exception e){
			return "";
		}
	}
}
