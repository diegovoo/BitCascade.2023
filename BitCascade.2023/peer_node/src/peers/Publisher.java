// Clase que implementa la interfaz remota Seed.
// Actúa como un servidor que ofrece un método remoto para leer los bloques
// del fichero publicado.
// LA FUNCIONALIDAD DE LA CLASE SE COMPLETA EN FASE 1 (TODO 1) Y LA 2 (TODO 2)

package peers;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import interfaces.Seed;
import interfaces.Tracker;

// Se comporta como un objeto remoto: UnicastRemoteObject
@SuppressWarnings("deprecation")
public class Publisher extends UnicastRemoteObject implements Seed {
    public static final long serialVersionUID = 1234567890L;
    String name; // nombre del nodo (solo para depurar)
    transient String file;
    String path; // convenio: path = name + "/" + file
    int blockSize;
    int numBlocks;

    // add
    RandomAccessFile randomAccessFile;

    public Publisher(String n, String f, int bSize) throws RemoteException, IOException {
        name = n; // nombre del nodo (solo para depurar)
        file = f; // nombre del fichero especificado
        path = name + "/" + file; // convenio: directorio = nombre del nodo
        blockSize = bSize; // tamaño de bloque especificado
        // Cálculo del nº bloques redondeado por exceso:
        // truco: ⌈x/y⌉ -> (x+y-1)/y
        numBlocks = (int) (new File(path).length() + blockSize - 1) / blockSize;

        // TODO 2: abrir el fichero para leer (RandomAccessFile)
       randomAccessFile = new RandomAccessFile(path, "r");
    }

    public String getName() throws RemoteException {
        return name;
    }

    public byte[] read(int numBl) throws RemoteException {
        int bufSize = blockSize;
        long fileSizeBytes = new File(path).length();
        if (numBl + 1 == numBlocks) {
            int fragmentSize = (int) (fileSizeBytes % blockSize);
            if (fragmentSize > 0)
                bufSize = fragmentSize;
        }
        byte[] buf = new byte[bufSize];
        if (numBl < numBlocks) {
            System.out.println("publisher read " + numBl);
            try {
                randomAccessFile.seek(numBl * blockSize);
                int n = randomAccessFile.read(buf);
            } catch (IOException e) {
                System.out.println("error mi loco");
            }
        }
        // TODO 2: realiza lectura solicitada devolviendo lo leido en buf
        // Cuidado con ultimo bloque que probablemente no estara completo
        return buf;
    }

    public int getNumBlocks() { // no es método remoto
        return numBlocks;
    }

    // Obtiene del registry una referencia al tracker y publica mediante
    // announceFile el fichero especificado creando una instancia de esta clase
    static public void main(String args[]) throws RemoteException {
        if (args.length != 5) {
            System.err.println("Usage: Publisher registryHost registryPort name file blockSize");
            return;
        }
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());

        try {
            // TODO 1: localiza el registry en el host y puerto indicado
            // y obtiene la referencia remota al tracker asignándola
            // a esta variable:
            Tracker trck = null;

            Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            trck = (Tracker) registry.lookup("mi_tracker");
            // comprobamos si ha obtenido bien la referencia:
            System.out.println("el nombre del nodo del tracker es: " + trck.getName());
            // TODO 1: crea un objeto de la clase Publisher y usa el método
            // remoto announceFile del Tracker para publicar el fichero
            // (nº bloques disponible en getNumBlocks de esa clase)
            //
            Publisher publisher = null;
            boolean res = false;

            if (trck.lookupFile(args[3]) == null) {
            publisher = new Publisher(args[2], args[3], Integer.parseInt(args[4]));
            res = trck.announceFile(publisher, args[3], Integer.parseInt(args[4]), publisher.getNumBlocks());
            }
        
            if (!res) { // comprueba resultado
                // si false: ya existe fichero publicado con ese nombre
                System.err.println("Fichero ya publicado");
                System.exit(1);
            }
            System.err.println("Dando servicio...");
            // no termina nunca (modo de operación de UnicastRemoteObject)
        } catch (Exception e) {
            System.err.println("Publisher exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
