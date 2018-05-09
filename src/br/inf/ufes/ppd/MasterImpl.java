package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MasterImpl implements Master 
{
	// Lista de escravos registrados no mestre
	Map<java.util.UUID, SlaveStatus> slaves = new HashMap<java.util.UUID, SlaveStatus>();
	
	// Lista de ataques em andamento
	Map<Integer, Attack> attacks = new HashMap<Integer, Attack>();
	
	// Lista de sub-ataques em andamento
	Map<Integer, SubAttack> subAttacks = new HashMap<Integer, SubAttack>();
	
	// Número (identificador) do último ataque
	Integer lastAttackNumber = 0;
	
	// Número (identificador) do último sub-ataque
	Integer lastSubattackNumber = 0;
	
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
		
		// Imprime aviso no mestre
		System.err.println(	"Escravo: "+slaves.get(slaveKey).getSlaveName()+
				", Índice atual: "+currentindex+
				", Palavra candidata encontrada: "+currentguess.getKey());

		// Descobre o número do ataque (attackNumber se refere ao sub-ataque)
		int numeroAttack = this.subAttacks.get(attackNumber).getAttackNumber();
		
		// Insere o guess na lista
		this.attacks.get(numeroAttack).guesses.add(currentguess);
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		
		// Busca escravo na lista
		SlaveStatus slave = slaves.get(slaveKey);
				
		// Calcula tempo gasto
		long horaInicio = this.subAttacks.get(attackNumber).getHoraInicio();
		long horaAtual = System.currentTimeMillis();
		long tempo = horaAtual - horaInicio;
		
		final long FATOR_SEGUNDO = 1000;
        final long FATOR_MINUTO = FATOR_SEGUNDO * 60;
        final long FATOR_HORA = FATOR_MINUTO * 60;
        
        long horas, minutos, segundos, milesimos;

        horas = tempo / FATOR_HORA;
        minutos = (tempo % FATOR_HORA) / FATOR_MINUTO;
        segundos = (tempo % FATOR_MINUTO) / FATOR_SEGUNDO;
        milesimos = tempo % FATOR_SEGUNDO;
		
		// Imprime aviso no mestre
		System.err.println(	"Escravo: "+slave.getSlaveName()+
				", Tempo decorrido: "+horas + " horas, " + minutos + " minutos, " + segundos + " segundos e " + milesimos + " milesimos"+
				", Índice atual: "+currentindex);
		
		try 
		{
			// Atualiza o currentindex
			this.subAttacks.get(attackNumber).setCurrentindex(currentindex);
		} 
		catch (Exception e) 
		{
			// Descobre o número do ataque (attackNumber se refere ao sub-ataque)
			int numeroAttack = this.subAttacks.get(attackNumber).getAttackNumber();
			
			try 
			{
				// Decrementa quantidade de subataques no ataque correspondente
				this.attacks.get(numeroAttack).decrementaSubataquesEmAndamento();
			} 
			catch (Exception e1) 
			{
				
			}
			
			// Remove subataque da lista
			this.subAttacks.remove(attackNumber);
		}
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Cria um ataque
		Attack attack = new Attack(lastAttackNumber++);
		
		// Adiciona o ataque na lista de ataques
		this.attacks.put(attack.getAttackNumber(), attack);
		
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
			// Cria um sub-ataque
			SubAttack subattack = new SubAttack(lastSubattackNumber++, attack.getAttackNumber(), indiceFinal);
			
			// Adiciona o sub-ataque na lista de sub-ataques
			this.subAttacks.put(subattack.getSubAttackNumber(), subattack);
			
			// Registra o sub-ataque no ataque
			attack.incrementaSubataquesEmAndamento();
			
			// Chama startSubAttack
			entry.getValue().getSlave().startSubAttack(ciphertext, knowntext, indiceInicial, indiceFinal, subattack.getSubAttackNumber(), this);
			
			// Atualiza os índices do dicionário para o próximo escravo
			indiceInicial = indiceFinal+1;
			indiceFinal = indiceInicial+divisao-1;
			if(mod>0) {indiceFinal++; mod--;}
		}
		
		// Espera o último checkpoint
		while(this.attacks.get(attack.getAttackNumber()).getQuantidadeSubataquesEmAndamento() > 0){}
		
		// Retorna as palavras candidatas encontradas neste ataque
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		
		// Remove attack da lista
		this.attacks.remove(attack.getAttackNumber());
		
		return guesses;
	}
}
