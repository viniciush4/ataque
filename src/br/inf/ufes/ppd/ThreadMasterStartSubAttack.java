package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.util.Map;

public class ThreadMasterStartSubAttack implements Runnable 
{
	Map.Entry<java.util.UUID, SlaveStatus> entry;
	byte[] ciphertext;
	byte[] knowntext;
	Integer indiceInicial;
	Integer indiceFinal;
	int attackNumber;
	Master m;
	
	public ThreadMasterStartSubAttack(
		Map.Entry<java.util.UUID, SlaveStatus> entry,
		byte[] ciphertext,
		byte[] knowntext,
		Integer indiceInicial,
		Integer indiceFinal,
		int attackNumber,
		Master m
	)
	{
		this.entry = entry;
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.indiceInicial = indiceInicial;
		this.indiceFinal = indiceFinal;
		this.attackNumber = attackNumber;
		this.m = m;
	}
	
	public void run()
	{
		try {
			entry.getValue().getSlave().startSubAttack(ciphertext, knowntext, indiceInicial, indiceFinal, attackNumber, m);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
