package com.hms.admin.servlet;

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
// are stored in the distributed Redis store, eliminating server affinity and
// enabling safe horizontal scaling across multiple application instances.
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Doctor;

/**
 * UpdateDoctorServlet – handles updating an existing doctor's details.
 *
 * cr-java-0065: Session state (successMsg / errorMsg feedback attributes) is
 * now stored in Amazon ElastiCache for Redis via Spring Session.  The standard
 * HttpSession API is unchanged; the springSessionRepositoryFilter registered in
 * web.xml intercepts every HttpSession call and transparently delegates to the
 * distributed Redis store configured in com.hms.config.SpringSessionConfig.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/updateDoctor")
public class UpdateDoctorServlet extends HttpServlet {

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
			String password = req.getParameter("password");

			//here need to get id also...for updating the doctor details
			//doctors will update based on respective doctor's id
			int id = Integer.parseInt(req.getParameter("id"));

			Doctor doctor = new Doctor(id, fullName, dateOfBirth, qualification, specialist, email, phone, password);

			DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

			boolean f = docDAO.updateDoctor(doctor);

			// cr-java-0065: HttpSession is now backed by Amazon ElastiCache for
			// Redis through Spring Session – feedback messages (successMsg /
			// errorMsg) are stored in the distributed Redis store, ensuring they
			// are visible regardless of which application instance handles the
			// subsequent request.
			HttpSession session = req.getSession(); // cr-java-0065: session backed by ElastiCache for Redis via Spring Session

			if (f == true) {
				// cr-java-0065: successMsg stored in Redis-backed distributed session
				session.setAttribute("successMsg", "Doctor update Successfully");
				resp.sendRedirect("admin/view_doctor.jsp");

			} else {
				// cr-java-0065: errorMsg stored in Redis-backed distributed session
				session.setAttribute("errorMsg", "Something went wrong on server!");
				resp.sendRedirect("admin/view_doctor.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
