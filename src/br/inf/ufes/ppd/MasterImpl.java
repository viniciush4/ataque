package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.UUID;

public class MasterImpl implements Master {
	
	public static void main(String args[]) 
	{	
		try 
		{
			// Cria referência de si para exportação
			Master objref = (Master) UnicastRemoteObject.exportObject(new MasterImpl(), 0);
			
			// Pega referência do registry
			Registry registry = LocateRegistry.getRegistry();
			
			// Faz o bind
			registry.rebind("mestre", objref);
			
			// Informa o status do mestre
			System.err.println("Master ready");
					
			
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		
		Guess guess = new Guess();
		guess.setKey("Chave teste");
		guess.setMessage("Mensagem teste".getBytes());
		

		Guess[] guesses = new Guess[1];
		guesses[0] = guess;
		
		return guesses;
	}

}
