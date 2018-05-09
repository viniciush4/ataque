package br.inf.ufes.ppd;

public class SubAttack {

	private long horaInicio = System.currentTimeMillis();
	private int attackNumber;
	private int subAttackNumber;
	private long currentindex;
	private long finalindex;
	
	public SubAttack(int subAttackNumber, int attackNumber, long finalindex) {
		this.subAttackNumber = subAttackNumber;
		this.attackNumber = attackNumber;
		this.finalindex = finalindex;
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
