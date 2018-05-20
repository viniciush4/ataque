package br.inf.ufes.mestre;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class MasterImpl implements Master 
{
	// Cores usadas para impressão no terminal
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_VERMELHO = "\u001B[31m";
	public static final String ANSI_VERDE = "\u001B[32m";
	public static final String ANSI_AMARELO = "\u001B[33m";
	public static final String ANSI_AZUL = "\u001B[34m";
	
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
			// Se não foi fornecido exatamente um argumento
			if(args.length < 1) {
				throw new Exception("Uso: MasterImpl <IP_DESTA_MÁQUINA>");
			}
			
			// Configura o hostname
			System.setProperty("java.rmi.server.hostname", args[0]);
			
			// Cria uma referência desta classe para exportação
			Master objref = (Master) UnicastRemoteObject.exportObject(new MasterImpl(), 0);
			
			// Pega referência do registry
			Registry registry = LocateRegistry.getRegistry("127.0.0.1");
			
			// Faz o bind
			registry.rebind("mestre", objref);
			
			// Informa o status do mestre
			System.err.println("[master] Ready");
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException 
	{
		synchronized(slaves) 
		{
			// Salva o escravo na lista de escravos (se existir, é substituido)
			slaves.put(slavekey, new SlaveStatus(slaveName, s));
		}
		
		// Imprime aviso no mestre
		System.err.println(ANSI_VERDE+"["+slaveName+"] Registrado"+ANSI_RESET);
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException 
	{
		synchronized(slaves) 
		{
			// Verifica se o escravo ainda está na lista
			if(this.slaves.get(slaveKey) != null)
			{
				// Guarda o nome do escravo
				String slaveName = this.slaves.get(slaveKey).getSlaveName();
				
				// Remove o escravo
				this.slaves.remove(slaveKey);
				
				// Imprime aviso no mestre
				System.err.println(ANSI_VERMELHO+"["+slaveName+"] Removido"+ANSI_RESET);
			}
		}
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess) 
			throws RemoteException {
		
		// Dados do sub-ataque
		long inicio;
		int numeroAttack;
		String slaveName;
		
		synchronized(subAttacks) 
		{
			// Busca a hora de início do sub-ataque
			inicio = subAttacks.get(attackNumber).getHoraInicio();

			// Busca o número do ataque (attackNumber se refere ao sub-ataque)
			numeroAttack = subAttacks.get(attackNumber).getAttackNumber();
		}
		
		// Busca o nome do escravo
		synchronized(slaves) { slaveName = slaves.get(slaveKey).getSlaveName(); }

		// Insere o guess na lista correspondente ao ataque
		synchronized(attacks) { attacks.get(numeroAttack).guesses.add(currentguess); }
		
		// Imprime aviso no mestre
		System.err.println(
			ANSI_AZUL+
			"["+slaveName+"] "+
			calcularTempoGasto(inicio)+
			" Índice: "+currentindex+
			" Palavra Candidata: "+currentguess.getKey()+
			ANSI_RESET
		);
		
		// Atualiza o currentindex
		synchronized(subAttacks) {subAttacks.get(attackNumber).setCurrentindex(currentindex);}
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException 
	{	
		// Dados do sub-ataque
		long inicio;
		String slaveName;

		// Busca a hora de início do sub-ataque
		synchronized(subAttacks) { inicio = subAttacks.get(attackNumber).getHoraInicio(); }
		
		// Busca o nome do escravo
		synchronized(slaves) { slaveName = slaves.get(slaveKey).getSlaveName();	}
		
		// Imprime aviso no mestre
		System.err.println(
			ANSI_AMARELO+
			"["+slaveName+"] "+
			calcularTempoGasto(inicio)+
			" Índice: "+currentindex+
			ANSI_RESET
		);
		
		// Atualiza o currentindex
		synchronized(subAttacks) {subAttacks.get(attackNumber).setCurrentindex(currentindex);}
	}
	
	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException 
	{
		// Aguarda ter ao menos um escravo na lista
		while(slaves.isEmpty()) 
		{
			System.err.println("[master] Aguardando um escravo para continuar.");
			
			try 
			{
				Thread.sleep(5000);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
				
		// Cria um ataque
		Attack attack = new Attack(lastAttackNumber++, ciphertext, knowntext);
		
		// Adiciona o ataque na lista de ataques
		synchronized(attacks) {	attacks.put(attack.getAttackNumber(), attack); }
		
		// Calcula os índices do dicionário para o primeiro escravo
		int quantidadeEscravos = slaves.size();
		int tamanhoDicionario = 80368;
		int divisao = (tamanhoDicionario / quantidadeEscravos);
		int mod = tamanhoDicionario % quantidadeEscravos;
		int indiceInicial = 0;
		int indiceFinal = divisao-1;
		if(mod>0) {indiceFinal++; mod--;}
		
		Map<Integer, SubAttack> subAttacksAseremRedistribuidos = new HashMap<Integer, SubAttack>();
		Map<java.util.UUID, SlaveStatus> slavesFalhos = new HashMap<java.util.UUID, SlaveStatus>();
		Map<java.util.UUID, SlaveStatus> copiaSlaves;
		
		// Copia a lista de escravos
		synchronized(slaves) { copiaSlaves = new HashMap<java.util.UUID, SlaveStatus>(slaves); }
		
		// Percorre os escravos
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : copiaSlaves.entrySet()) 
		{
			// Cria um sub-ataque
			SubAttack subattack = new SubAttack(lastSubattackNumber++, attack.getAttackNumber(), indiceInicial, indiceFinal, entry.getKey(), this);
			
			// Adiciona o sub-ataque na lista de sub-ataques
			synchronized(subAttacks) { subAttacks.put(subattack.getSubAttackNumber(), subattack); }
			
			// Registra o sub-ataque no ataque
			attack.incrementaSubataquesEmAndamento();
			
			try 
			{
				// Chama startSubAttack
				entry.getValue().getSlave().startSubAttack(ciphertext, knowntext, indiceInicial, indiceFinal, subattack.getSubAttackNumber(), this);
			} 
			catch (RemoteException e) 
			{
				// Adiciona na lista de slaves falhos
				slavesFalhos.put(entry.getKey(), entry.getValue());

				// Adiciona na lista de subattacks a serem redistribuidos
				subAttacksAseremRedistribuidos.put(subattack.getSubAttackNumber(), subattack);
				
				// Para o monitoramento do sub-ataque
				subattack.pararMonitoramento();
			}
			
			// Atualiza os índices do dicionário para o próximo escravo
			indiceInicial = indiceFinal+1;
			indiceFinal = indiceInicial+divisao-1;
			if(mod>0) {indiceFinal++; mod--;}
		}
		
		// Remove escravos falhos removondo-os
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : slavesFalhos.entrySet()) {
			removeSlave(entry.getKey());
		}
		
		// Redistribui os subattacks
		for(Map.Entry<Integer, SubAttack> entry : subAttacksAseremRedistribuidos.entrySet()) {
			redistribuirSubAttack(entry.getValue().getSubAttackNumber());
		}
		
		// Espera o último checkpoint (Enquanto a quantidade de sub-ataques em andamento é maior que zero)
		while(this.attacks.get(attack.getAttackNumber()).getQuantidadeSubataquesEmAndamento() > 0){}
		
		// Converte a lista de guesses em um array
		Guess[] guesses = new Guess[attacks.get(attack.getAttackNumber()).guesses.size()];
		attacks.get(attack.getAttackNumber()).guesses.toArray(guesses);
		
		// Remove attack da lista
		synchronized(attacks) { this.attacks.remove(attack.getAttackNumber()); }
		
		// Retorna os guesses
		return guesses;
	}

	protected void encerrarSubAttack(int subAttackNumber, int attackNumber) 
	{
		// Remove subataque da lista
		synchronized(subAttacks) {subAttacks.remove(subAttackNumber);}
		
		// Decrementa quantidade de subataques no ataque correspondente
		synchronized(attacks) {attacks.get(attackNumber).decrementaSubataquesEmAndamento();}
	}
	
	protected void redistribuirSubAttack(Integer subAttackNumber) throws RemoteException 
	{	
		// Aguarda ter ao menos um escravo na lista
		while(this.slaves.isEmpty()) 
		{
			System.err.println("[master] Aguardando um escravo para continuar.");
			
			try 
			{
				Thread.sleep(5000);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
		
		SubAttack subattackAntigo;
		Attack attack;
		
		synchronized(subAttacks) { subattackAntigo = subAttacks.get(subAttackNumber); }
		synchronized(attacks) { attack = attacks.get(subattackAntigo.getAttackNumber()); }
		
		// Calcula os índices do dicionário para o primeiro escravo
		int quantidadeEscravos = slaves.size();
		int tamanhoRange = (int) (subattackAntigo.getFinalindex() - subattackAntigo.getCurrentindex());
		int divisao = (tamanhoRange / quantidadeEscravos);
		int mod = tamanhoRange % quantidadeEscravos;
		int indiceInicial = (int) subattackAntigo.getCurrentindex() + 1;
		int indiceFinal = indiceInicial + divisao-1;
		if(mod>0) {indiceFinal++; mod--;}

		Map<Integer, SubAttack> subAttacksAseremRedistribuidos = new HashMap<Integer, SubAttack>();
		Map<java.util.UUID, SlaveStatus> slavesFalhos = new HashMap<java.util.UUID, SlaveStatus>();
		Map<java.util.UUID, SlaveStatus> copiaSlaves;
		
		// Copia a lista de escravos
		synchronized(slaves) { copiaSlaves = new HashMap<java.util.UUID, SlaveStatus>(slaves); }
		
		// Percorre os escravos
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : copiaSlaves.entrySet()) 
		{
			// Cria um sub-ataque
			SubAttack subattack = new SubAttack(lastSubattackNumber++, subattackAntigo.getAttackNumber(), indiceInicial, indiceFinal, entry.getKey(), this);
			
			// Adiciona o sub-ataque na lista de sub-ataques
			synchronized(subAttacks) { this.subAttacks.put(subattack.getSubAttackNumber(), subattack); }
			
			// Registra o sub-ataque no ataque
			attack.incrementaSubataquesEmAndamento();
			
			try 
			{
				// Chama startSubAttack
				entry.getValue().getSlave().startSubAttack(attack.getCiphertext(), attack.getKnowntext(), indiceInicial, indiceFinal, subattack.getSubAttackNumber(), this);
			} 
			catch (RemoteException e) 
			{
				// Adiciona na lista de slaves falhos
				slavesFalhos.put(entry.getKey(), entry.getValue());

				// Adiciona na lista de subattacks a serem redistribuidos
				subAttacksAseremRedistribuidos.put(subattack.getSubAttackNumber(), subattack);
				
				// Para o monitoramento do subattack
				subattack.pararMonitoramento();
			}
			
			// Atualiza os índices do dicionário para o próximo escravo
			indiceInicial = indiceFinal+1;
			indiceFinal = indiceInicial+divisao-1;
			if(mod>0) {indiceFinal++; mod--;}
		}
		
		// Decrementa a quantidade de sub-ataques em andamento
		synchronized(attacks) { attacks.get(subattackAntigo.getAttackNumber()).decrementaSubataquesEmAndamento(); }
		
		// Remove escravos falhos
		for(Map.Entry<java.util.UUID, SlaveStatus> entry : slavesFalhos.entrySet()) {
			removeSlave(entry.getKey());
		}
		
		// Redistribui os subattacks
		for(Map.Entry<Integer, SubAttack> entry : subAttacksAseremRedistribuidos.entrySet()) {
			redistribuirSubAttack(entry.getValue().getSubAttackNumber());
		}
	}
	
	private String calcularTempoGasto(long inicio) {
		
		long horaAtual = System.nanoTime();
		long tempo = horaAtual - inicio;
		
		final long FATOR_MICRO = 1000;
		final long FATOR_MILI = FATOR_MICRO * 1000;
		final long FATOR_SEGUNDO = FATOR_MILI * 1000;
        final long FATOR_MINUTO = FATOR_SEGUNDO * 60;
        final long FATOR_HORA = FATOR_MINUTO * 60;
        
        long horas, minutos, segundos, milesimos, micros;

        horas = tempo / FATOR_HORA;
        minutos = (tempo % FATOR_HORA) / FATOR_MINUTO;
        segundos = (tempo % FATOR_MINUTO) / FATOR_SEGUNDO;
        milesimos = (tempo % FATOR_SEGUNDO) / FATOR_MILI;
        micros = (tempo % FATOR_MILI) / FATOR_MICRO;
        
		return horas+":"+minutos+":"+segundos+":"+milesimos+":"+micros;
	}
}
