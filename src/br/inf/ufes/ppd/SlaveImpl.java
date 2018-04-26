package br.inf.ufes.ppd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SlaveImpl implements Slave 
{
	
	public static void main(String[] args)
	{
		try 
		{
			// Se não foi fornecido o primeiro argumento
			if(args[0].isEmpty()) {
				throw new Exception("Informe um nome para o escravo");
			}
			
			// Pega referência do registry
			Registry registry = LocateRegistry.getRegistry();
			
			// Faz lookup no mestre
			Master mestre = (Master) registry.lookup("mestre");
			
			// Cria referencia de si para exportação
			SlaveImpl obj = new SlaveImpl();
			Slave objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
			
			// Registra-se no mestre
			mestre.addSlave(objref, args[0], java.util.UUID.randomUUID());
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
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
	throws java.rmi.RemoteException{
		
		// Lê o arquivo do dicionário
		File arquivo = new File("../dictionary.txt");
				
		try {
			FileReader arq = new FileReader(arquivo);
			@SuppressWarnings("resource")
			BufferedReader lerArq = new BufferedReader(arq);
			
			System.err.println(lerArq.readLine());
			System.err.println(lerArq.readLine());
			System.err.println(lerArq.readLine());
			System.err.println(lerArq.readLine());
			System.err.println(lerArq.readLine());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
