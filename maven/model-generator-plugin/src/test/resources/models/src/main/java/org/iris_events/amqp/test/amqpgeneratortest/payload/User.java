
package org.iris_events.amqp.test.amqpgeneratortest.payload;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;


/**
 * This is a User schema component
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "age",
    "name",
    "status",
    "surname"
})
public class User implements Serializable
{

    /**
     * Age of the user
     * 
     */
    @JsonProperty("age")
    @JsonPropertyDescription("Age of the user")
    private int age;
    /**
     * Name of the user
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Name of the user")
    private String name;
    @JsonProperty("status")
    private Object status;
    /**
     * Surname of the user
     * 
     */
    @JsonProperty("surname")
    @JsonPropertyDescription("Surname of the user")
    private String surname;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
    private final static long serialVersionUID = -5906912075466638929L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public User() {
    }

    /**
     * 
     * @param surname
     *     Surname of the user.
     * @param name
     *     Name of the user.
     * @param age
     *     Age of the user.
     */
    public User(int age, String name, Object status, String surname) {
        super();
        this.age = age;
        this.name = name;
        this.status = status;
        this.surname = surname;
    }

    /**
     * Age of the user
     * 
     */
    @JsonProperty("age")
    public int getAge() {
        return age;
    }

    /**
     * Age of the user
     * 
     */
    @JsonProperty("age")
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Name of the user
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Name of the user
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("status")
    public Object getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(Object status) {
        this.status = status;
    }

    /**
     * Surname of the user
     * 
     */
    @JsonProperty("surname")
    public String getSurname() {
        return surname;
    }

    /**
     * Surname of the user
     * 
     */
    @JsonProperty("surname")
    public void setSurname(String surname) {
        this.surname = surname;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(User.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("age");
        sb.append('=');
        sb.append(this.age);
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("surname");
        sb.append('=');
        sb.append(((this.surname == null)?"<null>":this.surname));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+ this.age);
        result = ((result* 31)+((this.surname == null)? 0 :this.surname.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof User) == false) {
            return false;
        }
        User rhs = ((User) other);
        return ((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&(this.age == rhs.age))&&((this.surname == rhs.surname)||((this.surname!= null)&&this.surname.equals(rhs.surname))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
