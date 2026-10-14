package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065: HttpSession is retained as the API surface; Spring Session
// (configured in com.hms.config.SpringSessionConfig) transparently replaces
// the in-process session store with Amazon ElastiCache for Redis via the
// springSessionRepositoryFilter declared in web.xml.  All session attributes
// (successMsgForD, errorMsgForD, doctorObj) are stored in the distributed Redis
// store, eliminating server affinity and enabling safe horizontal scaling.
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * DoctorEditProfileServlet – handles doctor profile edit requests.
 *
 * cr-java-0065: Session state (successMsgForD / errorMsgForD / doctorObj) is
 * now stored in Amazon ElastiCache for Redis via Spring Session.  The standard
 * HttpSession API is unchanged; the springSessionRepositoryFilter registered in
 * web.xml intercepts every HttpSession call and transparently delegates to the
 * distributed Redis store configured in com.hms.config.SpringSessionConfig.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/doctorEditProfile")
public class DoctorEditProfileServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// get all data which is coming from doctor.jsp doctor details
			String fullName = req.getParameter("fullName");
			String dateOfBirth = req.getParameter("dateOfBirth");
			String qualification = req.getParameter("qualification");
			String specialist = req.getParameter("specialist");
			String email = req.getParameter("email");
			String phone = req.getParameter("phone");
			//String password = req.getParameter("password");

			int id = Integer.parseInt(req.getParameter("doctorId"));

			Doctor doctor = new Doctor(id, fullName, dateOfBirth, qualification, specialist, email, phone, "");

			DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

			boolean f = docDAO.editDoctorProfile(doctor);

			// cr-java-0065 (Line 42): HttpSession is now backed by Amazon ElastiCache for
			// Redis through Spring Session – all session attributes set below are
			// stored in the distributed Redis store, ensuring they are visible
			// regardless of which application instance handles the subsequent request.
			HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

			if (f == true) {
				Doctor updateDoctorObj = docDAO.getDoctorById(id);
				// cr-java-0065 (Lines 46-47): successMsgForD and doctorObj stored in Redis-backed distributed session
				session.setAttribute("successMsgForD", "Doctor update Successfully");
				session.setAttribute("doctorObj", updateDoctorObj); // over ride or update old session value to new updated doctor value.
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {
				// cr-java-0065 (Line 51): errorMsgForD stored in Redis-backed distributed session
				session.setAttribute("errorMsgForD", "Something went wrong on server!");
				resp.sendRedirect("doctor/edit_profile.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
