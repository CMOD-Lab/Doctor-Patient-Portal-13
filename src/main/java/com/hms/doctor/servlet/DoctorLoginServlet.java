package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065 (Line 10): HttpSession is retained as the API surface; Spring Session
// (configured in com.hms.config.SpringSessionConfig) transparently replaces
// the in-process session store with Amazon ElastiCache for Redis via the
// springSessionRepositoryFilter declared in web.xml.  All session attributes
// (doctorObj, errorMsg) are stored in the distributed Redis store, eliminating
// server affinity and enabling safe horizontal scaling.
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * DoctorLoginServlet – authenticates a doctor and establishes a session.
 *
 * cr-java-0065: Session state (doctorObj / errorMsg) is now stored in Amazon
 * ElastiCache for Redis via Spring Session.  The standard HttpSession API is
 * unchanged; the springSessionRepositoryFilter registered in web.xml intercepts
 * every HttpSession call and transparently delegates to the distributed Redis
 * store configured in com.hms.config.SpringSessionConfig.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/doctorLogin")
public class DoctorLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// get email and password which is coming from doctor_login.jsp page
		String email = req.getParameter("email");
		String password = req.getParameter("password");

		// cr-java-0065 (Line 29): HttpSession is now backed by Amazon ElastiCache for Redis
		// through Spring Session – the session object below is a Redis-backed
		// distributed session, not an in-process server-local session.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		// create DB connection
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

		// call loginDoctor() method for doctor login which method declared in DoctorDAO
		Doctor doctor = docDAO.loginDoctor(email, password);

		if (doctor != null) {
			// means doctor is valid or exist
			// then store particular logged in doctor object in session
			// cr-java-0065 (Line 40): doctorObj stored in Redis-backed distributed session
			session.setAttribute("doctorObj", doctor);
			// and redirect the particular doctor index page which is reside doctor folder
			resp.sendRedirect("doctor/index.jsp"); // doctor index means dashboard of doctors
		} else {
			// cr-java-0065 (Line 44): errorMsg stored in Redis-backed distributed session
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("doctor_login.jsp");
		}

	}

}
