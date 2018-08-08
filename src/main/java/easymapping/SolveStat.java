package easymapping;

public class SolveStat {
	int correctCount = 0;
	int unknownCount = 0;
	int incorrectCount = 0;

	public int getCorrectCount() {
		return correctCount;
	}

	public void setCorrectCount(int correctCount) {
		this.correctCount = correctCount;
	}

	public int getUnknownCount() {
		return unknownCount;
	}

	public void setUnknownCount(int unknownCount) {
		this.unknownCount = unknownCount;
	}

	public int getIncorrectCount() {
		return incorrectCount;
	}

	public void setIncorrectCount(int incorrectCount) {
		this.incorrectCount = incorrectCount;
	}

	public SolveStat(int correctCount, int unknownCount, int incorrectCount) {
		super();
		this.correctCount = correctCount;
		this.unknownCount = unknownCount;
		this.incorrectCount = incorrectCount;
	}

	public void add(SolveStat trySolve) {
		this.correctCount += trySolve.correctCount;
		this.incorrectCount += trySolve.incorrectCount;
		this.unknownCount += trySolve.unknownCount;
	}
}