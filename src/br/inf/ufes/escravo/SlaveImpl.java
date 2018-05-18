package br.inf.ufes.escravo;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;

public class SlaveImpl implements Slave 
{
	// Identificação única do escravo
	private static java.util.UUID slaveKey = java.util.UUID.randomUUID();
	
	// Nome do escravo
	private static String slaveName;
	
	// Referencia do escravo
	private static Slave objref;
	
	// Referencia do mestre
	private static Master mestre;
	
	// Referencia do registry
	private static Registry registry;
	
	
	public static void main(String[] args)
	{
		try 
		{
			// Se não foram fornecidos exatamente três argumentos
			if(args.length < 3) {
				throw new Exception("Uso: SlaveImpl <IP_DESTA_MÁQUINA> <IP_DO_MESTRE> <NOME_ESCRAVO>");
			}
					
			System.setProperty("java.rmi.server.hostname", args[0]);
			
			// Guarda o nome do escravo
			slaveName = args[2];
			
			// Pega referência do registry a partir do IP fornecido
			registry = LocateRegistry.getRegistry(args[1]);
			
			// Pega a referência do mestre no registry
			mestre = (Master) registry.lookup("mestre");
			
			// Cria uma referência de si para exportação
			objref = (Slave) UnicastRemoteObject.exportObject(new SlaveImpl(), 0);
			
			// Executa e Agenda a execução de addSlave
			final Timer t = new Timer();
			t.schedule(
				new TimerTask() 
			    {
			    	@Override
			        public void run() 
			    	{
			    		try 
			    		{
							executarAddSlave();
						} 
			    		catch (RemoteException e) 
			    		{
							e.printStackTrace();
						}
			    		catch (NotBoundException e)
			    		{
			    			System.err.println("O mestre não está ativo");
			    		}
			        }
			    }, 
    		0, 30000);
			
		}
		catch (NotBoundException e)
		{
			System.err.println("O mestre não está ativo");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void executarAddSlave() throws RemoteException, NotBoundException
	{
		try 
		{
			mestre.addSlave(objref, slaveName, slaveKey);
		}
		catch (ConnectException e) 
		{
			// Pega a referência do mestre no registry
			mestre = (Master) registry.lookup("mestre");
		}
	}
	
	/**
	 * Solicita a um escravo que inicie sua parte do ataque.
	 * @param ciphertext mensagem critografada
	 * @param knowntext trecho conhecido da mensagem decriptografada
	 * @param initialwordindex índice inicial do trecho do dicionário
	 * a ser considerado no sub-ataque.
	 * @param finalwordindex índice final do trecho do dicionário
	 * a ser considerado no sub-ataque.
     * @param attackNumber chave que identifica o ataque
	 * @param callbackinterface interface do mestre para chamada de
	 * checkpoint e foundGuess
	 * @throws java.rmi.RemoteException
	 */
	public void startSubAttack(
		byte[] ciphertext,
		byte[] knowntext,
		long initialwordindex,
		long finalwordindex,
		int attackNumber,
		SlaveManager callbackinterface)
	throws java.rmi.RemoteException 
	{		
		Thread t = new Thread(
			new ExecuteSubAttack(
				ciphertext, knowntext, initialwordindex, finalwordindex, attackNumber, callbackinterface, slaveKey
			)
		);
		t.start();
	}
}
