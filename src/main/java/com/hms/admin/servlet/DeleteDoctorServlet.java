package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

/**
 * DeleteDoctorServlet – session state is now stored in Amazon ElastiCache for
 * Redis via Spring Session.  The HttpSession API is unchanged; Spring Session
 * transparently replaces the in-process session store with a distributed Redis
 * store, eliminating server affinity and enabling horizontal scaling.
 *
 * Required configuration (application.properties / environment variables):
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 *
 * Spring Session is activated by @EnableRedisHttpSession on
 * com.hms.config.SpringSessionConfig.
 */
@WebServlet("/deleteDoctor")
public class DeleteDoctorServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//get id(which is coming as string value) and convert into int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DoctorDAO docDAO = new DoctorDAO(DBConnection.getConn());

		// HttpSession is now backed by Amazon ElastiCache for Redis through
		// Spring Session – feedback messages (successMsg / errorMsg) are stored
		// in the distributed Redis store, ensuring they are visible regardless
		// of which application instance handles the subsequent request.
		HttpSession session = req.getSession();
		
		boolean f = docDAO.deleteDoctorById(id);
		
		if(f==true) {
			session.setAttribute("successMsg", "Doctor Deleted Successfully.");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
		else {
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("admin/view_doctor.jsp");
		}
	}
	
	

}
