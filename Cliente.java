import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Cliente {
	
	//Función para manejar la autenticación del usuario
	//Devuelve el nombre de usuario autenticado en el servidor, "None" si no se ha podido realizar la autenticación con éxito o "-1" si se cancela la conexión
    private static String login(PrintWriter outPrinter, BufferedReader inReader){
        String opcion = "0";

        System.out.println("OPCIONES DEL LOGIN");
        System.out.println("1-Acceder");
        System.out.println("2-Registrarse");
		System.out.println("3-Cancelar");

        opcion = System.console().readLine();

        while(!(opcion.equals("1")|| opcion.equals("2") || opcion.equals("3"))){
            System.out.println("Opción incorrecta. Por favor introduzca una opción válida");
            opcion = System.console().readLine();
        }

		if(opcion.equals("3")){
			return "-1";
		}

        System.out.println("Introduzca su nombre de usuario:");
        String username = System.console().readLine();
        System.out.println("Introduzca su contraseña:");
        String passwd = System.console().readLine();

        String mensaje;

		//En Función de la opción elegida (registro o autenticación) se envía el mensaje pertinente al servidor
        if(opcion.equals("1")){
            mensaje = "101-LOGIN-LOGIN-" + username + "-PASSWORD-" + passwd;
        }
        else{
            mensaje = "100-REGISTER-LOGIN-" + username + "-PASSWORD-" + passwd;
        }

        outPrinter.println(mensaje);

        String respuesta = null;

        try {
            respuesta = inReader.readLine();
        } catch (IOException e) {
            System.err.println("Error al recibir la respuesta del servidor");
            return "None";
        }

        String partes[];
        partes = respuesta.split("-");

        if(partes[1].equals("ERROR")){
            System.out.println(partes[1] + " " + partes[2]);
            return "None";
        }

        return username;

    }

	//Función para manejar el registro de un nuevo pokemon
    private static void registrarPokemon(PrintWriter outPrinter, BufferedReader inReader){
        String pokemon, response = null;

		//Se introduce el nombre del pokemon a registrar
        System.out.println("Introduzca el nombre del pokemon a registrar:");
        pokemon = System.console().readLine();

		//Se envía al servidor
        outPrinter.println("102-POK-" + pokemon);

        try {
            response = inReader.readLine();
        } catch (IOException e) {
            System.err.println("Error al recibir la respuesta del servidor");
        }

        String partes[];
        partes = response.split("-");

		//Y se recoge y muestra la respuesta del servidor
        if(partes[1].equals("ERROR")){
            System.out.println(partes[1] + " " + partes[2]);
        }
        else{
            System.out.println("Se ha registrado a " + pokemon + " en la pokedex");
        }
        
    }

	//Función para el listado de los pokemons registrados
    private static void listarPokemon(PrintWriter outPrinter, BufferedReader inReader){
        outPrinter.println("103-LIST");	//Se manda la petición de listado al servidor
        String response;
        String partes[];
        Boolean seguir = true;

        System.out.println("Los pokemons recogidos en tu pokedex son:");

		//Y se recogen y muestran los nombres devueltos por el servidor
        while(seguir){
            try {
                response = inReader.readLine();
            } catch (IOException e) {
                System.err.println("Error al recibir la respuesta del servidor");
                break;
            }

            partes = response.split("-");
            if(partes[0].equals("203")){
                System.out.println(partes[2]);
            }
            else if(partes[0].equals("204")){
                seguir = false;	//Hasta que este indique el final del listado
            }
        }
    }

	public static void main(String[] args) {
		
		String cadenaRecibida;
		
		// Nombre del host donde se ejecuta el servidor:
		String host="localhost";
		// Puerto en el que espera el servidor:
		int port=8989;
		
		// Socket para la conexión TCP
        Socket socketServicio=null;

        String user = "None";

        String opcion = "0";
		
		try {
			// Creamos un socket que se conecte a "host" y "port":
			socketServicio = new Socket(host,port);

            PrintWriter outPrinter = new PrintWriter(socketServicio.getOutputStream(),true);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(socketServicio.getInputStream()));

			//Se realiza la función de login hasta que se autentifique un usuario o se cancele la conexión
            do{
                user = login(outPrinter,inReader);
            }while(user.equals("None"));

			//En caso de que se cancele la conexión, se envía el LOGOUT al servidor, se espera a su respuesta, se cierra el socket y se mata el proceso
			if(user.equals("-1")){
				outPrinter.println("105-LOGOUT");
				try {
					cadenaRecibida = inReader.readLine();
				} catch (IOException e) {
					System.out.println("Error al recibir la despedida del servidor");
				}
				socketServicio.close();
				return;
			}

			//Bucle principal del cliente. Se presetan las opciones disponibles y se llama a las funciones manejadoras de cada opcion según corresponda
            do{
                System.out.println("MENÚ");
                System.out.println("1-Registrar Pokemon");
                System.out.println("2-Ver Pokemon Conocidos");
                System.out.println("3-Cerrar Sesión");

                opcion = System.console().readLine();

                if(opcion.equals("1")){
                    registrarPokemon(outPrinter,inReader);
                }

                else if(opcion.equals("2")){
                    listarPokemon(outPrinter, inReader);
                }

                else if(!opcion.equals("3")){
                    System.out.println("Opción incorrecta. Por favor introduzca una opción válida");
                }

            }while(!opcion.equals("3"));

            outPrinter.println("105-LOGOUT");

            try {
                cadenaRecibida = inReader.readLine();
            } catch (IOException e) {
                System.out.println("Error al recibir la despedida del servidor");
            }
                
			// Una vez terminado el servicio, cerramos el socket (automáticamente se cierran
			// el inpuStream  y el outputStream)
			socketServicio.close();
			
			// Excepciones:
		} catch (UnknownHostException e) {
			System.err.println("Error: Nombre de host no encontrado.");
		} catch (IOException e) {
			System.err.println("Error de entrada/salida al abrir el socket.");
		}
	}
}
