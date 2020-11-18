import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.Socket;
import java.util.Scanner;

public class ProcesadorPokedex extends Thread{
	// Referencia a un socket para enviar/recibir las peticiones/respuestas
	private Socket socketServicio;
    // Referencia al servidor para consultar la lista de usuarios con sesión activa y el cerrojo
    private Servidor server;
    //Usuario con sesión activa en esta hebra
    private String user_logged = "None";
	
	// Constructor que tiene como parámetro una referencia al socket abierto en por otra clase y a un objeto servidor
	public ProcesadorPokedex(Socket socketServicio, Servidor server) {
        this.socketServicio=socketServicio;
        this.server = server;
	}
    
    private String autentificar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

        mensajes = cadena.split("-");
        String user = mensajes[3];
        String pass = mensajes[5];

        if(server.isLogged(user)){
            return "406-ERROR-Ya hay una sesión activa para este usuario";
        }

        Boolean user_correct = false, pass_correct = false;

        try {
            File fichero = new File("./Pokedex/users.pok");

            if(!fichero.exists()){
                return "402-ERROR-El usuario no existe";
            }

            server.lock.readLock().lock();
            try{
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
            } finally{
                server.lock.readLock().unlock();
            }
            
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

        String response = "201-OK-Login correcto";
        user_logged = user;
        server.login_usuario(user_logged);
        return response;

    }

    private String registrar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

        mensajes = cadena.split("-");
        String user = mensajes[3];

        String path = "./Pokedex/" + user;
        File directory = new File(path);
        Boolean dir_ok = directory.mkdirs();

        if(!dir_ok){
            String response = "401-ERROR-Ya existe este usuario";
            return response;
        }
        
        String pass = mensajes[5];

        try {
            server.lock.writeLock().lock();
            try{
                File users = new File("./Pokedex/users.pok");
                users.getParentFile().mkdirs();
                users.createNewFile();
            } finally {
                server.lock.writeLock().unlock();
            }

            
        } catch (Exception e) {
            System.err.println("Error al crear el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

        try {

            server.lock.writeLock().lock();
            try{
                FileWriter writer = new FileWriter("./Pokedex/users.pok",true);
                String user_pass = user + ":" + pass + "\n";
                writer.write(user_pass);
                writer.close();
            } finally {
                server.lock.writeLock().unlock();
            }
            
        } catch (Exception e) {
            System.err.println("Error al abrir el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

        String response = "200-OK-Usuario registrado";
        user_logged = user;
        server.login_usuario(user_logged);
        return response;
    }
    
    private String registrarPokemon(String pokemon){
        String response;

        try {
            File fichero = new File("./Pokedex/" + user_logged + "/" + pokemon + ".pok");

            if(fichero.createNewFile()){
                response = "202-OK-Pokemon registrado";
            }
            else{
                response = "405-ERROR-Pokemon ya registrado";
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo del pokemon");
            response = "499-ERROR-Error en el servidor";
        }

        return response;
    }

    private void listarPokemon(PrintWriter outPrinter){
        String filename, pokemon;
        File folder = new File("./Pokedex/" + user_logged + "/");
        File[] listOfFiles = folder.listFiles();

        for(File file : listOfFiles){
            if(file.isFile()){
                filename = file.getName();
                pokemon = filename.substring(0, filename.length()-4);
                outPrinter.println("203-POK-" + pokemon);
            }
        }

        outPrinter.println("204-OK-Listado completado");
    }

	// Aquí es donde se realiza el procesamiento realmente:
	void procesa(){
		
        String mensajeRecibido;
        Boolean servicio = true;
        String mensajes[];
        String response = null;
        		
		try {
			// Obtiene los flujos de escritura/lectura
			PrintWriter outPrinter = new PrintWriter(socketServicio.getOutputStream(),true);
			BufferedReader inReader = new BufferedReader(new InputStreamReader(socketServicio.getInputStream()));
            
            while(user_logged.equals("None")){
                mensajeRecibido = inReader.readLine();
                
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

            while(servicio){
                mensajeRecibido = inReader.readLine();
                mensajes = mensajeRecibido.split("-");

                if(mensajes[0].equals("102")){
                    response = registrarPokemon(mensajes[2]);
                    outPrinter.println(response);
                }

                if(mensajes[0].equals("103")){
                    listarPokemon(outPrinter);
                }

                else if(mensajes[0].equals("105")){
                    servicio = false;
                    response = "205-LOGOUT-Bye";
                }
            }

            server.logout_usuario(user_logged);
            outPrinter.println(response);
            			
		} catch (IOException e) {
			System.err.println("Error al obtener los flujos de entrada/salida.");
		}

    }
    
    public void run(){
        procesa();
    }
}