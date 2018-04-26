package br.inf.ufes.ppd;

public class SlaveStatus {

	private String slaveName;
	private Slave slave;
	private long currentindex;
	
	public SlaveStatus(String slaveName, Slave slave) {
		this.slaveName = slaveName;
		this.setSlave(slave);
	}

	public String getSlaveName() {
		return slaveName;
	}

	public void setSlaveName(String slaveName) {
		this.slaveName = slaveName;
	}

	public Slave getSlave() {
		return slave;
	}

	public void setSlave(Slave slave) {
		this.slave = slave;
	}

	public long getCurrentindex() {
		return currentindex;
	}

	public void setCurrentindex(long currentindex) {
		this.currentindex = currentindex;
	}
}
