package br.inf.ufes.ppd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SlaveImpl implements Slave 
{
	static java.util.UUID slaveKey = java.util.UUID.randomUUID();
	static long currentindex;
	static String slaveName;
	
	public static void main(String[] args)
	{
		try 
		{
			// Se não foi fornecido o primeiro argumento
			if(args[0].isEmpty()) 
			{
				throw new Exception("Informe um nome para o escravo");
			}
			
			slaveName = args[0];
			
			// Pega referência do registry
			Registry registry = LocateRegistry.getRegistry();
			
			// Faz lookup no mestre
			Master mestre = (Master) registry.lookup("mestre");
			
			// Cria referencia de si para exportação
			SlaveImpl obj = new SlaveImpl();
			Slave objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
			
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
			
			// Agenda a execução de checkpoint
			final Timer t2 = new Timer();
	        t2.schedule(
        		new TimerTask() 
		        {
		        	@Override
		            public void run() 
		        	{
		        		try 
		        		{
							executarCheckpoint(mestre);
						} 
		        		catch (RemoteException e) 
		        		{
							e.printStackTrace();
						}
	                }
	            }, 
	        3000, 3000);
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void executarCheckpoint(Master m) throws RemoteException
	{
		m.checkpoint(slaveKey, 1, currentindex);
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
		try 
		{
			System.err.println("Inicial: "+initialwordindex+" - Final: "+finalwordindex);
			
			// Lê o arquivo do dicionário
			File arquivo = new File("../dictionary.txt");
			FileReader arq = new FileReader(arquivo);
			@SuppressWarnings("resource")
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
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
