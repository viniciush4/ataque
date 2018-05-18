package br.inf.ufes.mestre;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import br.inf.ufes.ppd.SlaveManager;

public class SubAttack {

	private long horaInicio = System.currentTimeMillis();
	private long horaUltimoCheckpoint;
	private int attackNumber;
	private int subAttackNumber;
	private long currentindex;
	private long finalindex;
	java.util.UUID slaveKey;
	MasterImpl mestre;
	final Timer t;
	
	public SubAttack(int subAttackNumber, int attackNumber, long finalindex, java.util.UUID slaveKey, SlaveManager m) {
		this.subAttackNumber = subAttackNumber;
		this.attackNumber = attackNumber;
		this.finalindex = finalindex;
		this.mestre = (MasterImpl) m;
		this.slaveKey = slaveKey;
		this.horaUltimoCheckpoint = System.currentTimeMillis();
		
		// Agenda a execução de monitorarSubattack
		t = new Timer();
		t.schedule(
			new TimerTask() 
		    {
		    	@Override
		        public void run() 
		    	{
		    		try 
		    		{
		    			monitorarSubattack();
		    		} 
		    		catch(RemoteException e) 
		    		{
		    			e.printStackTrace();
		    		}
		        }
		    }, 
		0, 20000);
	}
	
	private void monitorarSubattack() throws RemoteException {
		
		long tempoDesdeOultimoCheckpoint = System.currentTimeMillis() - this.horaUltimoCheckpoint;
		
		// Se passou 20s desde o ultimo checkpoint
		if(tempoDesdeOultimoCheckpoint > 20000)
		{
			// Chama função do mestre para remover escravo
			this.mestre.removeSlave(slaveKey);
						
			// Distribui este subattack para outros escravos
			this.mestre.redistribuirSubAttack(this.subAttackNumber);
			
			// Para de monitorar este sub-ataque
			t.cancel();
		}
	}

	public long getHoraInicio() {
		return horaInicio;
	}

	public int getSubAttackNumber() {
		return subAttackNumber;
	}

	public long getCurrentindex() {
		return currentindex;
	}

	public void setCurrentindex(long currentindex) throws Exception {
		this.currentindex = currentindex;
		this.horaUltimoCheckpoint = System.currentTimeMillis();
		
		// Se for o último indice
		if(this.currentindex == this.finalindex) {
			throw new Exception();
		}
	}

	public int getAttackNumber() {
		return attackNumber;
	}

	public long getFinalindex() {
		return finalindex;
	}
}
