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
	// Modo Overhead
	private static boolean overhead;
	
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
			// Se não foram fornecidos pelo menos quatro argumentos
			if(args.length < 4) {
				throw new Exception("Uso: SlaveImpl <IP_DESTA_MÁQUINA> <IP_DO_MESTRE> <NOME_ESCRAVO> <HAB_MODO_OVERHEAD? 0-N | 1-S>");
			}
					
			// Configura o hostname
			System.setProperty("java.rmi.server.hostname", args[0]);
			
			// Guarda preferência do modo overhead
			overhead = (Integer.parseInt(args[3]) == 1) ? true : false;
			
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
			// Se registra no mestre
			mestre.addSlave(objref, slaveName, slaveKey);
		}
		catch (ConnectException e) 
		{
			// Pega a referência do mestre no registry
			mestre = (Master) registry.lookup("mestre");
		}
	}
	
	@Override
	public void startSubAttack(
		byte[] ciphertext,
		byte[] knowntext,
		long initialwordindex,
		long finalwordindex,
		int attackNumber,
		SlaveManager callbackinterface)
	throws java.rmi.RemoteException 
	{	
		// Se estiver em modo overhead
		if(overhead) 
		{
			// Retorna o último checkpoint (com o último índice)
			callbackinterface.checkpoint(slaveKey, attackNumber, finalwordindex);
		} 
		else 
		{
			// Cria uma thread para executar o sub-ataque
			Thread t = new Thread( new ExecuteSubAttack(
				ciphertext, knowntext, initialwordindex, finalwordindex, attackNumber, callbackinterface, slaveKey, slaveName
			));
			
			t.start();
		}
	}
}
