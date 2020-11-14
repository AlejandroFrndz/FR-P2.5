//
// YodafyServidorIterativo
// (CC) jjramos, 2012
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.Socket;
import java.util.Scanner;

//
// Nota: si esta clase extendiera la clase Thread, y el procesamiento lo hiciera el método "run()",
// ¡Podríamos realizar un procesado concurrente! 
//
public class ProcesadorPokedex {
	// Referencia a un socket para enviar/recibir las peticiones/respuestas
	private Socket socketServicio;
	// stream de lectura (por aquí se recibe lo que envía el cliente)
	private InputStream inputStream;
	// stream de escritura (por aquí se envía los datos al cliente)
    private OutputStream outputStream;
    
    private String response;
    private static String user_logged = "None";
	
	// Constructor que tiene como parámetro una referencia al socket abierto en por otra clase
	public ProcesadorPokedex(Socket socketServicio) {
		this.socketServicio=socketServicio;
	}
    
    private static String autentificar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

        mensajes = cadena.split("-");
        String user = mensajes[3];
        String pass = mensajes[5];

        Boolean user_correct = false, pass_correct = false;

        try {
            File fichero = new File("users.pok");
            Scanner reader = new Scanner(fichero);
            while(reader.hasNextLine() && !user_correct){
                String data = reader.nextLine();
                String partes[] = data.split(":");
                if(partes[0].equals(user)){
                    user_correct = true;
                    if(partes[1].equals(pass)){
                        pass_correct = true;
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error al abrir el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

        if(!user_correct){
            String response = "402-ERROR-El usuario no existe";
            return response;
        }

        if(!pass_correct){
            String response = "402-ERROR-Contraseña incorrecta";
            return response;
        }

        String response = "200-OK-Login correcto";
        user_logged = user;
        return response;

    }

    private static String registrar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

        mensajes = cadena.split("-");
        String user = mensajes[3];

        String path = "./" + user;
        File directory = new File(path);
        Boolean dir_ok = directory.mkdir();

        if(!dir_ok){
            String response = "401-ERROR-Ya existe este usuario";
            return response;
        }
        
        String pass = mensajes[5];

        try {
            File users = new File("users.pok");
            users.createNewFile();
            
        } catch (Exception e) {
            System.err.println("Error al crear el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

        try {
            FileWriter writer = new FileWriter("users.pok",true);
            String user_pass = user + ":" + pass + "\n";
            writer.write(user_pass);
            writer.close();
        } catch (Exception e) {
            System.err.println("Error al abrir el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

        String response = "200-OK-Usuario registrado";
        user_logged = user;
        return response;
    }
	
	// Aquí es donde se realiza el procesamiento realmente:
	void procesa(){
		
		String mensajeRecibido;
        
        System.out.println("Usuario Actual " + user_logged);
		
		try {
			// Obtiene los flujos de escritura/lectura
			PrintWriter outPrinter = new PrintWriter(socketServicio.getOutputStream(),true);
			BufferedReader inReader = new BufferedReader(new InputStreamReader(socketServicio.getInputStream()));
            
            while(user_logged.equals("None")){
                mensajeRecibido = inReader.readLine();
                
                String mensajes[];
                mensajes = mensajeRecibido.split("-");

                if(mensajes[0].equals("100")){
                    response = registrar(mensajeRecibido);
                }
                else if(mensajes[0].equals("101")){
                    response = autentificar(mensajeRecibido);
                }
                
                outPrinter.println(response);
            }

            System.out.println("Usuario logeado: " + user_logged);
            user_logged = "None";
            			
		} catch (IOException e) {
			System.err.println("Error al obtener los flujso de entrada/salida.");
		}

	}
}