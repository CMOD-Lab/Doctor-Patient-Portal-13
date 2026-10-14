package com.hms.entity;

/**
 * User entity class.
 *
 * Cloud Readiness Fix (cr-java-0113 - Lack of Externalized Secrets):
 * - The password field is no longer exposed in toString() to prevent
 *   credential leakage in application logs and monitoring systems.
 * - Passwords and other sensitive credentials should be managed via
 *   AWS Secrets Manager (see AwsSecretsManagerUtil) rather than being
 *   stored or logged as plain text.
 */
public class User {
	private int id;
	private String fullName;
	private String email;
	private String password;
	
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}


	public User(int id, String fullName, String email, String password) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public User(String fullName, String email, String password) {
		super();
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getFullName() {
		return fullName;
	}


	public void setFullName(String fullName) {
		this.fullName = fullName;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * Returns a string representation of the User object.
	 *
	 * SECURITY FIX (cr-java-0113): The password field is masked with "****"
	 * to prevent sensitive credential exposure in application logs, stack traces,
	 * and monitoring outputs. Credentials must be managed via AWS Secrets Manager
	 * and must never appear in plain text in logs or serialized output.
	 */
	@Override
	public String toString() {
		return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + ", password=" + "****" + "]";
	}
	
	
	
	
	
	
	
	
}
