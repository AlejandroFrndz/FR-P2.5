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
    
	//Función que maneja la autenticación de un usuario
    private String autentificar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

		//Se trocea la cadena recibida para obtener los credenciales de acceso del usuario
        mensajes = cadena.split("-");
        String user = mensajes[3];
        String pass = mensajes[5];

		//Se comprueba si el usuario ya tiene una sesión activa, en cuyo caso se informa de ello al usuario y no se continua
        if(server.isLogged(user)){
            return "406-ERROR-Ya hay una sesión activa para este usuario";
        }

        Boolean user_correct = false, pass_correct = false;

		//Acceso al archivo users.pok para comprobar los credenciales del usuario
        try {
            File fichero = new File("./Pokedex/users.pok");

			//Si el fichero no existe es que no hay usuarios registrados en el sistema, por lo que el usuario no existe
            if(!fichero.exists()){
                return "402-ERROR-El usuario no existe";
            }

            server.lock.readLock().lock();	//Adquisición de cerrojo de lectura para el acceso a users.pok para evitar acceder si alguna otra hebra está escribiendo
            try{
                Scanner reader = new Scanner(fichero);
				//Para cada linea del archivo se comprueba si el usuario coincide con el proporcionado, y despues si coincide su contraseña
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
				//Finalmente se cierra el fichero y se libera el cerrojo adquirido
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
		//Si todo ha ido bien, se establece el usuario en este hebra y se registra su sesión en el servidor
        user_logged = user;
        server.login_usuario(user_logged);
        return response;

    }

	//Función que maneja el registro de un nuevo usuario
    private String registrar(String mensaje){
        String mensajes[];
        String cadena = null;

        cadena = mensaje;

		//Comenzamos dividiendo el mensaje recibido y extraemos el nombre de usuario introducido
        mensajes = cadena.split("-");
        String user = mensajes[3];

		//Como primer paso intentamos crear el directorio para el usuario
        String path = "./Pokedex/" + user;
        File directory = new File(path);
        Boolean dir_ok = directory.mkdirs();	//Con mkdirs nos aseguramos que también se cree el directorio Pokedex/ si este no estaba ya

		//Si el directorio ya existía es que el usuairo ya está registrado
        if(!dir_ok){
            String response = "401-ERROR-Ya existe este usuario";
            return response;
        }
        
        String pass = mensajes[5];	//Extraer la contraseña del mensaje del cliente

		//Si no estaba registrado, creamos el archivo users.pok si no lo estaba
        try {
            server.lock.writeLock().lock();	//Adquisición del cerrojo de escritura para el funcionamiento concurrente de los procesadores
            try{
                File users = new File("./Pokedex/users.pok");
                users.getParentFile().mkdirs();
                users.createNewFile();
            } finally {
                server.lock.writeLock().unlock();	//Liberación del cerrojo
            }

            
        } catch (Exception e) {
            System.err.println("Error al crear el archivo de usuarios");
            String response = "499-ERROR-Error en el servidor";
            return response;
        }

		//A continuación escribimos las credenciales del usuario
        try {

            server.lock.writeLock().lock();	//De nuevo adquisición y liberación posterior del cerrojo de escritura
            try{
                FileWriter writer = new FileWriter("./Pokedex/users.pok",true);	//Con el segundo parámetro indicamos que el contenido a escrbir se añada al final del archivo sin sobreescribir la información ya contenida
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

		//Si todo ha ido bien preparamos la respuesta al cliente y marcamos la sesión del usuario correspondiente en esta hebra y en el servidor
        String response = "200-OK-Usuario registrado";
        user_logged = user;
        server.login_usuario(user_logged);
        return response;
    }
    
	//Función para manejar el registro de nuevos pokemon para el usuario con sesión activa en esta hebra
    private String registrarPokemon(String pokemon){
        String response;

		//Creamos un fichero .pok para el pokemon en el directorio del usuario
        try {
            File fichero = new File("./Pokedex/" + user_logged + "/" + pokemon + ".pok");

            if(fichero.createNewFile()){
                response = "202-OK-Pokemon registrado";
            }
            else{
                response = "405-ERROR-Pokemon ya registrado";	//Si el archivo ya existia quiere decir que el pokemon ya estaba registrado
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo del pokemon");
            response = "499-ERROR-Error en el servidor";
        }

        return response;
    }

	//Función para manejar el listado de los pokemons registrados por un usuario
    private void listarPokemon(PrintWriter outPrinter){
        String filename, pokemon;
        File folder = new File("./Pokedex/" + user_logged + "/");
        File[] listOfFiles = folder.listFiles();

		//Leemos los contenidos del directorio correspondiente al usuario
        for(File file : listOfFiles){
            if(file.isFile()){
                filename = file.getName();
                pokemon = filename.substring(0, filename.length()-4);	//Eliminamos el .pok del nombre de los archivos
                outPrinter.println("203-POK-" + pokemon);	//Y enviamos cada pokemon registrado al usuario
            }
        }

        outPrinter.println("204-OK-Listado completado");	//Cuando el listado ha finalizado, enviamos el mensaje de finalización al cliente
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
            
			//El primer paso es identificar, o registrar, al usuario. También se da la opción de cancelar la conexión.
            while(user_logged.equals("None")){
                mensajeRecibido = inReader.readLine();
                
                mensajes = mensajeRecibido.split("-");

                if(mensajes[0].equals("100")){
                    response = registrar(mensajeRecibido);
                }
                else if(mensajes[0].equals("101")){
                    response = autentificar(mensajeRecibido);
                }
				else if(mensajes[0].equals("105")){
					response = "205-LOGOUT-Bye";	//Si se cancela la operación mandamos la despedida al cliente y salimos del bucle sin usuario
					outPrinter.println(response);
					break;
				}
                
                outPrinter.println(response);
            }

            if(user_logged.equals("None")){	//Si del bucle de identificación no ha salido ningún usuario quiere decir que la conexión se ha cancelado, por lo que matamos la hebra
				return;
			}

			//Bucle principal del procesamiento. Se permanecerá en él hasta que el usuario cierre la sesión
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