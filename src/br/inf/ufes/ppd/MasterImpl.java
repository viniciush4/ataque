package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MasterImpl implements Master 
{
	Map<java.util.UUID, SlaveStatus> slaves = new HashMap<java.util.UUID, SlaveStatus>();
	ArrayList<Attack> attacks = new ArrayList<Attack>();
	int lastAttackNumber = 0;
	
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
			e.printStackTrace();
		}
	}

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		this.slaves.put(slavekey, new SlaveStatus(slaveName, s));
		
		System.err.println("Escravo registrado: "+this.slaves.get(slavekey).getSlaveName());
		
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		
		System.err.println("Palavra candidata encontrada: "+currentguess.getKey());
		
		this.attacks.get(attackNumber).guesses.add(currentguess);
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		
		System.err.println("Checkpoint do escravo: "+slaveKey.toString());
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		
		// Cria um attack e adiciona-o na lista
		Attack attack = new Attack(lastAttackNumber++);
		this.attacks.add(attack.getAttackNumber(), attack);
		
		// Dividir o dicionario para os escravos
		Integer quantidadeEscravos = slaves.size();
		Integer tamanhoDicionario = 80368;
		
		// Percorre os escravos
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : slaves.entrySet()) {
			entry.getValue().getSlave().startSubAttack(ciphertext, knowntext, 0, tamanhoDicionario-1, attack.getAttackNumber(), this);
		}
		
		// Retorna os guess encontrados neste ataque
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		return guesses;
	}
}
