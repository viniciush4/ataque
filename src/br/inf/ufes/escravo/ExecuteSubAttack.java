package br.inf.ufes.escravo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.SlaveManager;

public class ExecuteSubAttack implements Runnable 
{
	// Cores usadas para impressão no terminal
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_VERMELHO = "\u001B[31m";
	public static final String ANSI_VERDE = "\u001B[32m";
	public static final String ANSI_AMARELO = "\u001B[33m";
	public static final String ANSI_AZUL = "\u001B[34m";
	
	// Atributos
	byte[] ciphertext;
	byte[] knowntext;
	long initialwordindex;
	long finalwordindex;
	int attackNumber;
	SlaveManager callbackinterface;
	long currentindex;
	java.util.UUID slaveKey;
	String slaveName;
	final Timer temporizadorCheckpoint;
		
	// Construtor
	public ExecuteSubAttack(
		byte[] ciphertext,
		byte[] knowntext,
		long initialwordindex,
		long finalwordindex,
		int attackNumber,
		SlaveManager callbackinterface,
		java.util.UUID slaveKey,
		String slaveName
	)
	{
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.initialwordindex = initialwordindex;
		this.finalwordindex = finalwordindex;
		this.attackNumber = attackNumber;
		this.callbackinterface = callbackinterface;
		this.slaveKey = slaveKey;
		this.slaveName = slaveName;
		
		// Agenda a execução de checkpoint
		temporizadorCheckpoint = new Timer();
        temporizadorCheckpoint.schedule(
    		new TimerTask() 
	        {
	        	@Override
	            public void run() 
	        	{
	        		try 
	        		{
						executarCheckpoint();
					} 
	        		catch (RemoteException e) 
	        		{
						e.printStackTrace();
					}
                }
            }, 
        10000, 10000);
	}
	
	// Executa um checkpoint
	public void executarCheckpoint() throws RemoteException
	{
		callbackinterface.checkpoint(slaveKey, attackNumber, currentindex);
	}

	@Override
	public void run() 
	{
		try 
		{
			// Imprime no escravo os índices inicial e final
			System.err.println(ANSI_VERDE+"["+slaveName+"] Índices: "+initialwordindex+" .. "+finalwordindex+ANSI_RESET);
			
			// Lê o arquivo do dicionário
			File arquivo = new File("../dictionary.txt");
			FileReader arq = new FileReader(arquivo);
			BufferedReader lerArq = new BufferedReader(arq);
			
			// Avança até initialwordindex
			for(long i=0;i<initialwordindex;i++) { lerArq.readLine(); }
			
			// Percorre o intervalo solicitado no dicionario
			for(long i=initialwordindex; i<=finalwordindex;i++) 
			{
				// Lê a palavra candidata
				String palavra = lerArq.readLine();
				byte[] decrypted = null;
				
				try
				{
					// Usa a palavra para descriptografar o ciphertext
					byte[] key = palavra.getBytes();
					SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, keySpec);
					decrypted = cipher.doFinal(ciphertext);
				} 
				catch (javax.crypto.BadPaddingException e) 
				{
					// Atualiza currentindex
					currentindex = i;
					
					continue;
				}
				
				// Converte texto conhecido e mensagem descriptografada para String
				String mensagem_descriptografada = new String(decrypted, "UTF-8");
				String texto_conhecido = new String(knowntext, "UTF-8");
				
				// Verifica se o knowntext existe no texto descriptografado
				if(mensagem_descriptografada.contains(texto_conhecido)) 
				{
					// Avisa ao mestre 
					Guess currentguess = new Guess();
					currentguess.setKey(palavra);
					currentguess.setMessage(decrypted);
					callbackinterface.foundGuess(slaveKey, attackNumber, i, currentguess);
					
					// Imprime no escravo os índices inicial e final
					System.err.println(ANSI_AZUL+"["+slaveName+"] Índice: "+i+" Palavra Candidata: "+currentguess.getKey()+ANSI_RESET);
				}
				
				// Atualiza currentindex
				currentindex = i;
			}
			
			// Fecha o arquivo do dicionario
			arq.close();
			
			// Encerra o temporizador do checkpoint
			this.temporizadorCheckpoint.cancel();
			
			// Envia último checkpoint
			executarCheckpoint();
			
			// Imprime no escravo aviso de fim
			System.err.println(ANSI_AMARELO+"["+slaveName+"] Índice: "+currentindex+" Fim do sub-ataque"+ANSI_RESET);
			
		} 
		catch (Exception e) 
		{
			e.getMessage();
		}
	}
}
