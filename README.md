# FR-P2.5
Autores: Alejandro Fernández Alcaide [GitHub](https://github.com/AlejandroFrndz) y Víctor Díaz Bustos [GitHub](https://github.com/victordiazb00)\
Mensajes:

    ERROR:
        401-ERROR-Ya existe este usuario
        402-ERROR-El usuario no existe
        405-ERROR-Pokemon ya registrado
        406-ERROR-Ya hay una sesión activa para este usuario
        499-ERROR-Error en el servidor

    OK:
       200-OK-Usuario registrado
       201-OK-Login correcto 
       202-OK-Pokemon registrado
       203-POK-<pokemon>
       204-OK-Listado completado
       205-LOGOUT-Bye

    PETICIONES:
        100-REGISTER-LOGIN-<usuario>-PASSWORD-<contraseña>
        101-LOGIN-LOGIN-<usuario>-PASSWORD-<contraseña>
        102-POK-<pokemon>
        103-LIST
        105-LOGOUT
