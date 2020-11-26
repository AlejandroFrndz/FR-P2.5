import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.net.ServerSocket;

public class Servidor {
	private ArrayList<String> usuarios_activos = new ArrayList<String>();	//Lista que recoge los usuarios con sesión activa

	public ReadWriteLock lock = new ReentrantReadWriteLock();	//Cerrojo para el acceso concurrente a users.pok

	//Registrar una nueva sesión activa
	public void login_usuario(String usuario){
		usuarios_activos.add(usuario);
	}

	//Finalizar una sesión activa
	public void logout_usuario(String usuario){
		usuarios_activos.remove(usuario);
	}

	//Comprobar si un usuario determinado tiene una sesión activa
	public Boolean isLogged(String usuario){
		return usuarios_activos.contains(usuario);
	}

	public static void main(String[] args) {
	
		// Puerto de escucha
		int port=8989;

		Servidor server = new Servidor();
		
		try {
			// Abrimos el socket en modo pasivo, escuchando el en puerto indicado por "port"
			ServerSocket socketServidor;
			socketServidor = new ServerSocket(port);
			
			Socket socketServicio = null;

			// Mientras ... siempre!
			do {
				
				// Aceptamos una nueva conexión con accept()
				try {
					socketServicio = socketServidor.accept();
				} catch (IOException e){
					System.out.println("Error: no se pudo aceptar la conexión solicitada");
				}
				
				//Creamos un procesador, pasandole como parámetros el socket donde servir al cliente y el objeto servidor para las sesiones y el cerrojo compartido por todos
				ProcesadorPokedex procesador=new ProcesadorPokedex(socketServicio,server);
                procesador.start();
				
			} while (true);
			            			
		} catch (IOException e) {
			System.err.println("Error al escuchar en el puerto "+port);
		}

	}

}