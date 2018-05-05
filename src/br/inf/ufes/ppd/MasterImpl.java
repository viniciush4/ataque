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
	// Lista de escravos registrados no mestre
	Map<java.util.UUID, SlaveStatus> slaves = new HashMap<java.util.UUID, SlaveStatus>();
	
	// Lista de ataques em andamento
	ArrayList<Attack> attacks = new ArrayList<Attack>();
	
	// Número (identificador) do último ataque
	int lastAttackNumber = 0;
	
	public static void main(String args[]) 
	{	
		try 
		{
			// Cria uma referência desta classe para exportação
			Master objref = (Master) UnicastRemoteObject.exportObject(new MasterImpl(), 0);
			
			// Pega referência do registry
			Registry registry = LocateRegistry.getRegistry();
			
			// Faz o bind
			registry.rebind("mestre", objref);
			
			// Informa o status do mestre
			System.err.println("Master ready");
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		
		// Salva o escravo na lista de escravos (se existir, é substituido)
		this.slaves.put(slavekey, new SlaveStatus(slaveName, s));
		
		// Imprime aviso no mestre
		System.err.println("Escravo registrado: "+this.slaves.get(slavekey).getSlaveName());
		
	}

	@Override
	public synchronized void removeSlave(UUID slaveKey) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		
		System.err.println(	"Escravo: "+slaves.get(slaveKey).getSlaveName()+
				", Índice atual: "+slaves.get(slaveKey).getCurrentindex()+
				", Palavra candidata encontrada: "+currentguess.getKey());
		
		this.attacks.get(attackNumber).guesses.add(currentguess);
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		
		// Busca escravo na lista
		SlaveStatus slave = slaves.get(slaveKey);
		
		// Salva o estado do escravo
		slave.setCurrentindex(currentindex);
		
		// Imprime aviso no mestre
		System.err.println(	"Escravo: "+slave.getSlaveName()+
				", Tempo decorrido: TEMPO"+
				", Índice atual: "+slave.getCurrentindex());
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Cria um ataque
		Attack attack = new Attack(lastAttackNumber++);
		
		// Adiciona o ataque na lista de ataques
		this.attacks.add(attack.getAttackNumber(), attack);
		
		// Calcula os índices do dicionário para o primeiro escravo
		int quantidadeEscravos = slaves.size();
		int tamanhoDicionario = 80368;
		int divisao = (tamanhoDicionario / quantidadeEscravos);
		int mod = tamanhoDicionario % quantidadeEscravos;
		int indiceInicial = 0;
		int indiceFinal = divisao-1;
		if(mod>0) {indiceFinal++; mod--;}
		
		// Percorre os escravos
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : slaves.entrySet()) 
		{
			// Cria um sub-ataque dentro do ataque
			attack.subatacks.add(new SubAttack(entry.getKey()));
			
			// Chama startSubAttack
			entry.getValue().getSlave().startSubAttack(ciphertext, knowntext, indiceInicial, indiceFinal, attack.getAttackNumber(), this);
			
			// Atualiza os índices do dicionário para o próximo escravo
			indiceInicial = indiceFinal+1;
			indiceFinal = indiceInicial+divisao-1;
			if(mod>0) {indiceFinal++; mod--;}
		}
		
		// Espera o último checkpoint
		
		// Retorna as palavras candidatas encontradas neste ataque
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		return guesses;
	}
}
