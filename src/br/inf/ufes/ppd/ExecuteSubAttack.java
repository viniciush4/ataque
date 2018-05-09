package br.inf.ufes.ppd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ExecuteSubAttack implements Runnable 
{
	byte[] ciphertext;
	byte[] knowntext;
	long initialwordindex;
	long finalwordindex;
	int attackNumber;
	SlaveManager callbackinterface;
	long currentindex;
	java.util.UUID slaveKey;
	final Timer temporizadorCheckpoint;
		
	public ExecuteSubAttack(
		byte[] ciphertext,
		byte[] knowntext,
		long initialwordindex,
		long finalwordindex,
		int attackNumber,
		SlaveManager callbackinterface,
		java.util.UUID slaveKey
	)
	{
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.initialwordindex = initialwordindex;
		this.finalwordindex = finalwordindex;
		this.attackNumber = attackNumber;
		this.callbackinterface = callbackinterface;
		this.slaveKey = slaveKey;
		
		// Agenda a execução de checkpoint
		this.temporizadorCheckpoint = new Timer();
        this.temporizadorCheckpoint.schedule(
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
        3000, 3000);
	}
	
	public void executarCheckpoint() throws RemoteException
	{
		this.callbackinterface.checkpoint(slaveKey, attackNumber, currentindex);
	}

	@Override
	public void run() {
		
		try 
		{
			System.err.println("Inicial: "+initialwordindex+" - Final: "+finalwordindex);
			
			// Lê o arquivo do dicionário
			File arquivo = new File("../dictionary.txt");
			FileReader arq = new FileReader(arquivo);
			BufferedReader lerArq = new BufferedReader(arq);
			
			// Avança até initialwordindex
			for(long i=0;i<initialwordindex;i++) {
				lerArq.readLine();
			}
			
			// Percorre o intervalo solicitado no dicionario
			for(long i=initialwordindex; i<=finalwordindex;i++) 
			{
				// Lê a palavra candidata
				String palavra = lerArq.readLine();
				byte[] decrypted = null;
				
				// Usa a palavra para descriptografar o ciphertext
				try
				{
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
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
