package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

public class SlaveImpl implements Slave 
{
	// Identificação única do escravo
	static java.util.UUID slaveKey = java.util.UUID.randomUUID();
	
	// Nome do escravo
	static String slaveName;
	
	
	public static void main(String[] args)
	{
		try 
		{
			// Se não foram fornecidos exatamente dois argumentos
			if(args.length < 2) {
				throw new Exception("Uso: SlaveImpl <IP_DO_MESTRE> <NOME_ESCRAVO>");
			}
			
			// Guarda o nome do escravo
			slaveName = args[1];
			
			// Pega referência do registry a partir do IP fornecido
			Registry registry = LocateRegistry.getRegistry(args[0]);
			
			// Pega a referência do mestre no registry
			Master mestre = (Master) registry.lookup("mestre");
			
			// Cria uma referência de si para exportação
			Slave objref = (Slave) UnicastRemoteObject.exportObject(new SlaveImpl(), 0);
			
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
							executarAddSlave(mestre, objref);
						} 
			    		catch (RemoteException e) 
			    		{
							e.printStackTrace();
						}
			        }
			    }, 
    		0, 3000);
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void executarAddSlave(Master m, Slave s) throws RemoteException
	{
		m.addSlave(s, slaveName, slaveKey);
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
