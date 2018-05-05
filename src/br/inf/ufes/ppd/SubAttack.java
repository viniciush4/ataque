package br.inf.ufes.ppd;

public class SubAttack {

	private static long horaInicio = System.currentTimeMillis();
	private java.util.UUID slaveKey;
	
	public SubAttack(java.util.UUID slaveKey) {
		this.slaveKey = slaveKey;
	}

	public static long getHoraInicio() {
		return horaInicio;
	}

	public java.util.UUID getSlaveKey() {
		return slaveKey;
	}
}
