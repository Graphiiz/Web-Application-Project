package finalproject.entities;

public class Person implements java.io.Serializable {

	private static final long serialVersionUID = 4190276780070819093L;

	// this is a person object that you will construct with data from the DB
	// table. The "sent" column is unnecessary. It's just a person with
	// a first name, last name, age, city, and ID.
	private String first;
	private String last;
	private int age;
	private String city;
	private int id;
	
	public Person() {
		super();
	}

	public Person(String first, String last, int age, String city, int id) {
		super();
		this.first = first;
		this.last = last;
		this.age = age;
		this.city = city;
		this.id = id;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Person [first=" + first + ", last=" + last + ", age=" + age + ", city=" + city + ", id=" + id + "]";
	}
}
