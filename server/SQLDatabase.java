/*
Copyright (c) 2002-2011 "Yapastream,"
Yapastream [http://yapastream.com]

This file is part of Yapastream.

Yapastream is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



public class SQLDatabase {
	private static final String URL = "jdbc:mysql://localhost:3306/yapastream";
	private static final String USER = "yapastream";
	private static final String PASSWORD = "y@p@str3@m";
	private static final String DRIVER = "com.mysql.jdbc.Driver";
	private Connection con = null;
	public SQLDatabase() {
		
	}
	 public Connection getConnection() throws SQLException {
		if (con == null) {
	      try {
	         Class.forName(DRIVER); 
	         con = DriverManager.getConnection(URL, USER, PASSWORD);
	      } catch(ClassNotFoundException e) {
			
	         System.out.println("Driver not found. Be sure CLASSPATH is set to mysql connector (eg. export CLASSPATH=/usr/share/java/mysql-connector-java-5.1.17-bin.jar:$CLASSPATH) " + e.getMessage());
	         return null;
	      }
	 	} else {
	 		if (con.isClosed()) {
	 			try {
	 		         Class.forName(DRIVER); 
	 		         con = DriverManager.getConnection(URL, USER, PASSWORD);
				 
	 		      }
	 		      catch(ClassNotFoundException e) {
	 		         System.out.println("Driver not found. Be sure CLASSPATH is set to mysql connector (eg. export CLASSPATH=/usr/share/java/mysql-connector-java-5.1.17-bin.jar:$CLASSPATH) " + e.getMessage());
	 		         return null;
	 		      }
	 		} 
	 	}
	    return con;
	  }

	 // true if username and password match
	 public boolean verifyUser(String username, String password) {
		 System.out.println("Verifying user in SQL database");
		 //"SELECT id FROM `users` WHERE username = ? AND password = ?"
		 ResultSet rs = null;
		 PreparedStatement s = null;
		 boolean retval = false;
		 String pass = null;
	      try {
	         s = getConnection().prepareStatement("SELECT password FROM `users` WHERE username = ?");
	         s.setString(1, username.toLowerCase());
	         //s.setString(2, password); AND password = ?
	         rs = s.executeQuery();
	         if (rs.next()) { 
	        	 pass = rs.getString("password");
	         }
	         if ((password.compareTo(pass) == 0) && (pass != null)) { // password matches
	        	 retval = true;
	         } 
	      }
	      catch(Exception e) {
	         System.out.println(e.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	  }
	      return retval;
	   }
	 public boolean removeStreams() {
		 int rs = 0;
		 PreparedStatement s = null;
		boolean retval = false;
	      try {
	         s = getConnection().prepareStatement("TRUNCATE TABLE `streams`");
	         rs = s.executeUpdate();
	         //if (rs.rowDeleted() == true) retval = true; 
	      }
	      catch(SQLException e) {
	         System.out.println(e.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	  }
	      return retval;
	   }
	   public boolean removeSettings() {
		int rs = 0;
		PreparedStatement s = null;
		boolean retval = false;
	      try {
	         s = getConnection().prepareStatement("TRUNCATE TABLE `settings`");
	         rs = s.executeUpdate();
	         //if (rs.rowDeleted() == true) retval = true; 
	      }
	      catch(SQLException e) {
	         System.out.println(e.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	  }
	      return retval;
	   }
	 public boolean joinUser(String username, String sessionId) {
		 int rs = 0;
		 PreparedStatement s = null;
	      try {
	         s = getConnection().prepareStatement("INSERT INTO `streams` (username, create_date, active, session_id) VALUES (?,?,?,?)");
	         s.setString(1, username.toLowerCase());
	         s.setString(2,  String.valueOf(System.currentTimeMillis()));
	         s.setInt(3, 1);
	         s.setString(4, sessionId);
	         rs = s.executeUpdate();
	        // if (!(rs.isAfterLast())) return true; 
	      }
	      catch(SQLException e) {
	         System.out.println(e.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	  }
	      return false;
	   }
	 public boolean partUser(String username, String sessionId) {
		 ResultSet rs = null;
		 int rsUpdate = 0;
		 PreparedStatement s = null;
		 PreparedStatement u = null;
		 int id=0;
	      try {
		  System.out.println("Selecting ID and parting user");
	         s = getConnection().prepareStatement("SELECT id FROM streams WHERE session_id=? AND username=? AND active=?");
	         s.setString(1, sessionId);
	         s.setString(2, username.toLowerCase());
	         s.setInt(3, 1);
	         rs = s.executeQuery();
	         while (rs.next()) {
	        	 id = rs.getInt("id");
	        	 u =  getConnection().prepareStatement("DELETE FROM `streams` WHERE id=?");
	        	// u =  getConnection().prepareStatement("UPDATE `streams` SET active=? WHERE id=?");// possibly/probably drop later??
	        	 //u.setInt(1, 0);
	        	 u.setInt(1, id);
	        	 rsUpdate = u.executeUpdate();
	         }
	      }
	      catch(SQLException e) {
	         System.out.println(e.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	  }
	      return false;
	   }
	   public boolean addSetting(String username, String settingName, String value) {
		 ResultSet rs = null;
		 int rsUpdate = 0;
		 PreparedStatement s = null;
		 PreparedStatement u = null;
		 int id=0;
	      try {
	         /*s = getConnection().prepareStatement("SELECT id FROM streams WHERE username=? AND active=?");
	         s.setString(1, username.toLowerCase());
	         s.setInt(2, 1);
	         rs = s.executeQuery();
			 System.out.println("no next" + username + " " + settingName  + " " + value);
	         while (rs.next()) {
				System.out.println("Got next");
	        	 id = rs.getInt("id");*/
			String insertStr = null;
			if (settingName.compareTo("privacy") == 0) {
				insertStr = "UPDATE `settings` SET privacy=? WHERE username=?";
				u = getConnection().prepareStatement(insertStr);
				u.setInt(1, Integer.parseInt(value)); // may throw numberformatexception
				u.setString(2, username);
				rsUpdate = u.executeUpdate();
					
				System.out.println("updated " + rsUpdate);
					if (rsUpdate == 0) {// if update does not update any rows then insert
						insertStr = "INSERT INTO `settings` (privacy, username) VALUES (?, ?)";
						u =  getConnection().prepareStatement(insertStr);
						u.setInt(1, Integer.parseInt(value)); // may throw numberformatexception
						u.setString(2, username);
						rsUpdate = u.executeUpdate();
					}
				}
	         //}
	      } catch(SQLException e) {
	         System.out.println(e.getMessage());
		  } catch (Exception ex) {
			System.out.println(ex.getMessage());
	      } finally {
	   		  try {
	   			  if (s != null) s.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   		  try {
	   			  if (con != null) con.close();
	   		  } catch (Exception ex) {
	   			  System.out.println("Exception:" + ex.getMessage());
	   		  }
	   	}
		System.out.println("returning");
	   return false;
	}
}

/*DAO dao = new DAO();
dao.getEmployees();*/

/*
// true if successful
public boolean signUp(String username, String password, String email, String location) {
     ResultSet rs = null;
     try {
   	  PreparedStatement s = null;
   	  try {
   		  s = getConnection().prepareStatement("INSERT INTO users (username, password, email, location) VALUES (?, ?, ?, ?);");
   		  s.setString(1, username);
   		  s.setString(2, password);
   		  s.setString(3, email);
   		  s.setString(4, location);
   	  }
   	  finally {
   		  try {
   			  if (s != null) s.close();
   		  } catch (Exception ex) {
   			  System.out.println("Exception:" + ex.getMessage());
   		  }
   		  try {
   			  if (conn != null) conn.close();
   		  } catch (Exception ex) {
   			  System.out.println("Exception:" + ex.getMessage());
   		  }
   	  }
   	  if (rs.rowInserted()) return true;
   	  else return false;
  }
public boolean forgotUsername(String email) {
	 //  ("SELECT username FROM `users` WHERE email=?");
	 // send email to email address containing username
	 String username;
	 ResultSet rs = null;
	 try {
	  PreparedStatement s = getConnection().prepareStatement("SELECT username FROM `users` WHERE email=?");
        s.setString(1, email);
        
        rs = s.executeQuery();
       
        if (!(rs.next()) { // should have a result
           username = rs.getString("username");
           // send email to email with username in email
         }
        
        
        
     } catch(SQLException e) {
        System.out.println(e.getMessage());
     } finally {
		  try {
			  if (s != null) s.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
		  try {
			  if (conn != null) conn.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
	  }
 
}
public boolean forgotPassword(String username) {
	 // INSERT INTO passwordConfirmation (username, confirmationCode) VALUES (?,?)
	 //  ("SELECT email FROM `users` WHERE username=?");
	 ResultSet rs = null;
	 String email = null;
     try {
   	  // generate confirmation code
   	 String confirmationCode = UUID.randomUUID().toString();
        PreparedStatement s = getConnection().prepareStatement("INSERT INTO passwordConfirmation (username, confirmationCode) VALUES (?,?)");
        s.setString(1, username);
        s.setString(2, confirmationCode);
        
        
        conn.prepareStatement("SELECT email FROM `users` WHERE username=?");
        s.setString(1, username);
        rs = s.executeQuery();
        if (rs.next()) { // only should have one result
           email = rs.getString("email");
           // send email with confirmation code
           
         }
     } catch(SQLException e) {
        System.out.println(e.getMessage());
     } finally {
		  try {
			  if (s != null) s.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
		  try {
			  if (conn != null) conn.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
	  }
    
     return false;
}
public boolean resetPassword(String username, String password, String confirmationCode) {
	 // SELECT id FROM passwordConfirmation WHERE username=? AND confirmationCode=?
	 ResultSet rs = null;
	 ResultSet rsValid = null;
	 int valid = null;
     try {
   	  // generate confirmation code
   	 String confirmationCode = UUID.randomUUID().toString();
        PreparedStatement s = getConnection().prepareStatement("SELECT valid,create_date FROM passwordConfirmation WHERE username=? AND confirmationCode=?");
        s.setString(1, username);
        s.setString(2, confirmationCode);
        rs = s.executeQuery();
        Date create_date;
        if (!(rs.next()) { // should have a result
           valid = rs.getInt("valid");
           create_date = rs.getDate("create_date");
           if ((valid == 1) && (date > now-3days)) {
           	// set to invalid
           	s = conn.prepareStatement("UPDATE passwordConfirmation SET valid=? WHERE username=? AND confirmationCode=?");
           	s.setInt(1, 0);
           	s.setString(2, username);
           	s.setString(3, confirmationCode);
           	updatePassword(username, password);
           }
           
         }
     } catch(SQLException e) {
        System.out.println(e.getMessage());
     } finally {
		  try {
			  if (s != null) s.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
		  try {
			  if (conn != null) conn.close();
		  } catch (Exception ex) {
			  System.out.println("Exception:" + ex.getMessage());
		  }
	  }
    
     return false;
}
public boolean updatePassword(String username, String password) {
	 // ("UPDATE `users` SET password=? WHERE username=?");
	 ResultSet rs = null;
     try {
   	  PreparedStatement s = null;
   	  try {
   		  s = getConnection().prepareStatement("UPDATE `users` SET password=? WHERE username=?");
   		  s.setString(1, username);
   		  s.setString(2, password);
   	  }
   	  finally {
   		  try {
   			  if (s != null) s.close();
   		  } catch (Exception ex) {
   			  System.out.println("Exception:" + ex.getMessage());
   		  }
   		  try {
   			  if (conn != null) conn.close();
   		  } catch (Exception ex) {
   			  System.out.println("Exception:" + ex.getMessage());
   		  }
   	  }
   	  if (rs.rowUpdated()) return true;
   	  else return false;
	 
}
*/
