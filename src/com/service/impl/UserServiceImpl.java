package com.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;

import com.beans.TrainException;
import com.beans.UserBean;
import com.constant.ResponseCode;
import com.constant.UserRole;
import javax.mail.Session;
import com.service.UserService;
import com.utility.DBUtil;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Transport;

public class UserServiceImpl implements UserService {

	private final String TABLE_NAME;

	public UserServiceImpl(UserRole userRole) {
		if (userRole == null) {
			userRole = UserRole.CUSTOMER;
		}
		this.TABLE_NAME = userRole.toString();
	}

	@Override
	public UserBean getUserByEmailId(String customerEmailId) throws TrainException {
		UserBean customer = null;
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE MAILID=?";
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, customerEmailId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				customer = new UserBean();
				customer.setFName(rs.getString("fname"));
				customer.setLName(rs.getString("lname"));
				customer.setAddr(rs.getString("addr"));
				customer.setMailId(rs.getString("mailid"));
				customer.setPhNo(rs.getLong("phno"));
			} else {
				throw new TrainException(ResponseCode.NO_CONTENT);
			}
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customer;
	}

	@Override
	public List<UserBean> getAllUsers() throws TrainException {
		List<UserBean> customers = null;
		String query = "SELECT * FROM  " + TABLE_NAME;
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			customers = new ArrayList<UserBean>();
			while (rs.next()) {
				UserBean customer = new UserBean();
				customer.setFName(rs.getString("fname"));
				customer.setLName(rs.getString("lname"));
				customer.setAddr(rs.getString("addr"));
				customer.setMailId(rs.getString("mailid"));
				customer.setPhNo(rs.getLong("phno"));
				customers.add(customer);
			}

			if (customers.isEmpty()) {
				throw new TrainException(ResponseCode.NO_CONTENT);
			}
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customers;
	}

	@Override
	public String updateUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		String query = "UPDATE  " + TABLE_NAME + " SET FNAME=?,LNAME=?,ADDR=?,PHNO=? WHERE MAILID=?";
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, customer.getFName());
			ps.setString(2, customer.getLName());
			ps.setString(3, customer.getAddr());
			ps.setLong(4, customer.getPhNo());
			ps.setString(5, customer.getMailId());
			int response = ps.executeUpdate();
			if (response > 0) {
				responseCode = ResponseCode.SUCCESS.toString();
			}
			ps.close();
		} catch (SQLException | TrainException e) {
			responseCode += " : " + e.getMessage();
		}
		return responseCode;
	}

	@Override
	public String deleteUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		String query = "DELETE FROM " + TABLE_NAME + " WHERE MAILID=?";
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, customer.getMailId());

			int response = ps.executeUpdate();
			if (response > 0) {
				responseCode = ResponseCode.SUCCESS.toString();
			}
			ps.close();
		} catch (SQLException | TrainException e) {
			responseCode += " : " + e.getMessage();
		}
		return responseCode;
	}

	@Override
	@PostMapping("/registerUser")
	public String registerUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		String query = "INSERT INTO " + TABLE_NAME + " VALUES(?,?,?,?,?,?,?)";
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			
			String otp = generateOTP();
	        customer.setOtp(otp);

	        System.out.println("This is the mail ID: " + customer.getMailId());
	        ps.setString(1, customer.getMailId());
	        ps.setString(2, customer.getPWord());
	        ps.setString(3, customer.getFName());
	        ps.setString(4, customer.getLName());
	        ps.setString(5, customer.getAddr());
	        ps.setLong(6, customer.getPhNo());
	        
	        ps.setString(7, customer.getOtp());
			//executeQuery
			System.out.println(TABLE_NAME);
			//ResultSet rs = ps.executeQuery();
			int rs= ps.executeUpdate();
			//rs.next()
			if (rs>0) {
				UserServiceImpl.sendEmail(customer.getMailId(),"SwiftRails Email Confirmation",customer.getOtp());
				responseCode = ResponseCode.SUCCESS.toString();
			}
			ps.close();
		} catch (Exception e) {
		    e.printStackTrace(); // Add this line to print the stack trace
		    if (e.getMessage().toUpperCase().contains("ORA-00001")) {
		        responseCode += " : " + "User With Id: " + customer.getMailId() + " is already registered ";
		    } else {
		        responseCode += " : " + e.getMessage();
		        System.out.println(responseCode);
		    }
		}

		return responseCode;
	}
	
	private static void sendEmail(String toEmail, String subject, String body) throws Exception {
	    // Sender's email address and password
	    String fromEmail = "maddinenidheeraj14@gmail.com";
	    String password = "Rojarani@10";

	    // Setup mail server properties
	    Properties properties = new Properties();
	    properties.put("mail.smtp.host", "smtp.gmail.com");
	    properties.put("mail.smtp.port", "587");
	    properties.put("mail.smtp.auth", "true");
	    properties.put("mail.smtp.starttls.enable", "true");
	    properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

	    // Get the Session object
	    Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
	        protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
	            return new javax.mail.PasswordAuthentication(fromEmail, password);
	        }
	    });

	    try {
	        // Create a default MimeMessage object
	        MimeMessage message = new MimeMessage(session);

	        // Set the sender and recipient addresses
	        message.setFrom(new InternetAddress(fromEmail));
	        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

	        // Set the email subject and body
	        message.setSubject(subject);
	        message.setText(body);

	        // Send the email
	        Transport.send(message);

	        System.out.println("Email sent successfully!");

	    } catch (Exception e) {
	        throw new Exception("Error sending email: " + e.getMessage());
	    }
	}

	// New method to generate a random OTP
	private String generateOTP() {
	    int otp = (int) ((Math.random() * 900000) + 100000);
	    return String.valueOf(otp);
	}
	private void sendOTPEmail(String email, String otp) {
	    // Implement the logic to send OTP to the user's email
	    // Use the email-sending code from the previous response
	    // Make sure to secure your email sending mechanism
	    // Example:
	    try {
	        String subject = "Your OTP for Registration";
	        String body = "Your OTP for registration is: " + otp;

	        sendEmail(email, subject, body);
	    } catch (Exception e) {
	        System.out.println("Error sending OTP email: " + e.getMessage());
	    }
	}


	@Override
	public UserBean loginUser(String username, String password) throws TrainException {
		UserBean customer = null;
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE MAILID=? AND PWORD=?";
		try {
			Connection con = DBUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ps.setString(1, username);
			ps.setString(2, password);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				customer = new UserBean();
				customer.setFName(rs.getString("fname"));
				customer.setLName(rs.getString("lname"));
				customer.setAddr(rs.getString("addr"));
				customer.setMailId(rs.getString("mailid"));
				customer.setPhNo(rs.getLong("phno"));
				customer.setPWord(rs.getString("pword"));
			} else {
				throw new TrainException(ResponseCode.UNAUTHORIZED);
			}
			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customer;
	}

}
