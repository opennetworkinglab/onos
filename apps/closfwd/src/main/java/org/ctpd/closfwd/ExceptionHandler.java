package org.ctpd.closfwd;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MediaType;
 
@Provider
public class ExceptionHandler implements ExceptionMapper<CustomiceException>
{
    @Override
    public Response toResponse(CustomiceException exception)
    {
       
        return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build(); 
       
    }
    
}
