import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Cliente {

    private static String login(PrintWriter outPrinter, BufferedReader inReader){
        String opcion = "0";

        System.out.println("OPCIONES DEL LOGIN");
        System.out.println("1-Acceder");
        System.out.println("2-Registrarse");

        opcion = System.console().readLine();

        while(!(opcion.equals("1")|| opcion.equals("2"))){
            System.out.println("Opción incorrecta. Por favor introduzca una opción válida");
            opcion = System.console().readLine();
        }

        System.out.println("Introduzca su nombre de usuario:");
        String username = System.console().readLine();
        System.out.println("Introduzca su contraseña:");
        String passwd = System.console().readLine();

        String mensaje;

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

    private static void registrarPokemon(PrintWriter outPrinter, BufferedReader inReader){
        String pokemon, response = null;

        System.out.println("Introduzca el nombre del pokemon a registrar:");
        pokemon = System.console().readLine();

        outPrinter.println("102-POK-" + pokemon);

        try {
            response = inReader.readLine();
        } catch (IOException e) {
            System.err.println("Error al recibir la respuesta del servidor");
        }

        String partes[];
        partes = response.split("-");

        if(partes[1].equals("ERROR")){
            System.out.println(partes[1] + " " + partes[2]);
        }
        else{
            System.out.println("Se ha registrado a " + pokemon + " en la pokedex");
        }
        
    }

    private static void listarPokemon(PrintWriter outPrinter, BufferedReader inReader){
        outPrinter.println("103-LIST");
        String response;
        String partes[];
        Boolean seguir = true;

        System.out.println("Los pokemons recogidos en tu pokedex son:");
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
            else{
                seguir = false;
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

            do{
                user = login(outPrinter,inReader);
            }while(user.equals("None"));

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
