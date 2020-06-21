package com.gromit25.presspublisher.formatter;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import org.reflections.Reflections;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.gromit25.presspublisher.formatter.flow.AbstractSubFlowFormatter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Formatter xml 설정을 읽어와서 구성함
 * 부모 Formatter에 자식 Formatter 목록을 가지는 형태로 만듦
 * Formatter 수행시 목록의 formatter를 순서대로 수행함
 * 
 * root formatter는 format 태그로 시작하여야 함
 * 
 * ex) 
 * flowformatter(하위 formatter가 리스트 형태로 저장됨)
 *    -> setformatter
 *    -> printformatter
 *    -> foreachformatter(하위 formatter가 리스트 형태로 저장됨)
 *          -> flowformatter
 *          -> printformatter
 *          -> printformatter
 *    -> printformatter
 * 
 * @author jmsohn
 */
public abstract class FormatterXmlHandler extends DefaultHandler {

	/** 생성 가능한 formatter의 목록 */
	@Getter(value=AccessLevel.PRIVATE)
	@Setter(value=AccessLevel.PRIVATE)
	private HashMap<String, Class<?>> formatterTypes = new HashMap<String, Class<?>>();

	@Getter(value=AccessLevel.PRIVATE)
	@Setter(value=AccessLevel.PRIVATE)
	private HashMap<Class<?>, Method> attrSetters = new HashMap<Class<?>, Method>();
	
	/** 부모 Formatter stack */
	@Getter(value=AccessLevel.PROTECTED)
	private Stack<Formatter> parentFormatterStack = new Stack<Formatter>();
	
	/** parsing 중인 현재 Formatter */
	@Getter
	@Setter(value=AccessLevel.PROTECTED)
	private Formatter formatter;
	
	/** 현재 parsing 위치 */
	@Getter(value=AccessLevel.PROTECTED)
	@Setter(value=AccessLevel.PRIVATE)
	private Locator locator;

	/**
	 * 사용할 Formatter 그룹명을 반환
	 * @return 사용할 Formatter 그룹명
	 */
	protected abstract Set<String> getFormatterGroupNames();
	
	/**
	 * 하위 flow를 가지는 Formatter의 하위 flow를 설정함
	 * 
	 * ex) 
	 *   excel formatter에서는
	 *   excel tag 자체를 처리하는 flow(BasicFlowFormatter)와
	 *   excel cell의 text를 처리하는 flow(TextFlowFormatter)가 나누어져 있어,
	 *   상황에 따라 flow 클래스 type을 지정하여 생성할 수 있도록 함
	 * 
	 * @param formatter 하위 flow를 가지는 Formatter
	 */
	protected abstract void setSubBasicFlow(AbstractSubFlowFormatter formatter) throws Exception;
	
	/**
	 * 생성자
	 */
	public FormatterXmlHandler() throws Exception {
		
		// 초기화 수행
		//
		Reflections reflect = new Reflections("com.epozen.publisher.formatter");
		
		this.loadFormatterTypes(reflect);
		this.loadFormatterAttrSetter(reflect);
	}
	
	/**
	 * Formatter의 타입을 로딩
	 *   사용할 그룹명(getFormatterGroupNames())에 해당하는
	 *   생성 가능한 formatter를 가져와 목록(formatterTypes)에 추가함
	 *   
	 * @param reflect 
	 */
	private void loadFormatterTypes(Reflections reflect) throws Exception {
		
		if(reflect == null) {
			throw new Exception("reflect is null.");
		}
		
		//
		Set<String> groupSet = this.getFormatterGroupNames();
		if(groupSet == null) {
			return;
		}
		
		//
		reflect
			.getTypesAnnotatedWith(FormatterSpec.class)
			.forEach(clazz -> {
				
				//
				FormatterSpec spec = clazz.getAnnotation(FormatterSpec.class);
				
				//
				if(spec != null	&& groupSet.contains(spec.group()) == true) {
					this.getFormatterTypes().put(spec.tag(), clazz);
				}
			});
	}
	
	/**
	 * Formatter의 속성(Attr) 설정을 위한 Setter 메소드를 로딩(type(class), method)
	 * 
	 * @param reflect
	 */
	private void loadFormatterAttrSetter(Reflections reflect) throws Exception {
		
		if(reflect == null) {
			throw new Exception("reflect is null.");
		}
		
		reflect
			.getTypesAnnotatedWith(FormatterAttrSetterClass.class)
			.forEach(setterClass -> {
				
				Method[] methods = setterClass.getDeclaredMethods();
				
				for(Method method : methods) {
					
					FormatterAttrSetter spec = method.getAnnotation(FormatterAttrSetter.class);
					if(spec == null) {
						continue;
					}
					
					for(Class<?> type: spec.value()) {
						this.getAttrSetters().put(type, method);
					}
				}
			});
	}
	
	@Override
	public void setDocumentLocator(Locator locator) {
		/** 현재 parsing 중인 위치 객체 설정 */
		this.setLocator(locator);
	}
	
	@Override
	public void startElement(String uri, String localName,
				String qName, Attributes attributes) throws SAXException {
		
		// 상위 Formatter에 하위 Formatter 리스트를 만들기 위해 
		// 현재 parsing되고 있는  formatter의 상위 Formatter를 
		// stack(formatters)에 보관하는 방식으로 parsing 수행함
		
		// ex)
		// <format>
		//    <set name="a" exp="value1"/>   
		//    <print exp="value1"/>                 <--(1)
		//    <foreach element="e1" list="valueList"/> 
		//          <print exp="e1.getName()"/>
		//          <print exp="e1.getAddress()"/>  <--(2)
		//    </foreach>
		// </format>
		// 상기와 같이 format이 들어 왔다면
		// (1)에서의 stack : flowformat(<format>)
		// (2)에서의 stack : flowformat(<format>), foreachformat
		// 이 됨
		
		// parsing 중인 formatter가 있으면,
		// 현재 formatter의 상위 formatter 이므로,
		// 상위 formatter stak에 push 함
		if(this.getFormatter() != null) {
			this.getParentFormatterStack().push(this.getFormatter());
		}
		
		// 테그(qName)에 해당하는 formatter를 만들어
		// parsing 중인 formatter로 설정함
		try {
			
			// 테그에 해당하는 formatter 생성
			this.setFormatter(this.createFormatter(qName));
			
			// 하위 flow를 가지는 formatter일 경우,
			// 하위 flow를 설정함
			if(this.getFormatter() instanceof AbstractSubFlowFormatter) {
				this.setSubBasicFlow((AbstractSubFlowFormatter)this.getFormatter());
			}
			
			// formatter 테그명
			this.getFormatter().setTagName(qName);
			
			// formatter에 테그 종료 위치 설정
			this.getFormatter().setLineNumber(this.getLocator().getLineNumber());
			this.getFormatter().setColumnNumber(this.getLocator().getColumnNumber());
			
			//
			this.processFormatterAttrAnnotation(this.getFormatter(), attributes);
			
			// formatter에 속성 값 설정
			// 추상함수로 구현된 Formatter에서 설정 작업 수행
			this.getFormatter().setAttributes(attributes);
			
		} catch(Exception ex) {
			throw new SAXException(ex);
		}
	}
	
	@Override
    public void endElement (String uri, String localName, String qName) throws SAXException {
		
		try {
			
			if(this.getParentFormatterStack().isEmpty() == false) {
				
				// 스택에 저장된 Formatter가 있는 경우
				//
				// parsing 중인 Formatter의 parsing이 완료 되었기 때문에
				// 부모 Formatter의 자식 Formatter 목록에
				// parsing 중인 Formatter를 등록하고,
				//
				// parsing 중인 Formatter는 부모 Formatter로 설정함
				Formatter parentFormatter = this.getParentFormatterStack().pop();
				parentFormatter.addChildFormatter(this.getFormatter());
				this.setFormatter(parentFormatter);
			}
			
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new SAXException(ex.getMessage());
		}
    }
    
	@Override
    public void characters (char ch[], int start, int length) throws SAXException {
    	
		/////////////////////
		// tag 내의 텍스트 처리
		
		try {
	    	String text = new String(ch, start, length);
	    	if(this.getFormatter() != null) {
	    		this.getFormatter().addText(text);
	    	}
		} catch(Exception ex) {
			throw new SAXException(ex.getMessage());
		}
    }
	
	/**
	 * tag 이름(qName)에 따라 Formatter를 생성하여 반환 
	 * @param qName tag 이름(qName)
	 * @return 생성된 Formatter
	 */
	private Formatter createFormatter(String qName) throws Exception {

		Class<?> formatterType = this.getFormatterTypes().get(qName);
		if(formatterType == null) {
			throw new Exception("unrecognized xml tag:" + qName);
		}
		
		return Formatter.class.cast(formatterType.newInstance());
	}
	
	/**
	 * XML tag의 속성(Attr) 값들을 Formatter에 설정함
	 * 
	 * @param formatter 설정할 Formatter
	 * @param attributes 속성(Attr) 목록
	 */
	private void processFormatterAttrAnnotation(Formatter formatter, Attributes attributes) throws Exception {
		
		if(formatter == null) {
			throw new Exception("formatter is null.");
		}
		
		if(attributes == null) {
			throw new Exception("attributes is null.");
		}
		
		for(Field field : formatter.getClass().getDeclaredFields()) {
			
			// 클래스의 각 필드별로 FormatterAttr 어노테이션이 있는지 확인함
			// FormatterAttr 어노테이션이 있는 경우,  
			// FormatterAttr에 설정된 이름(name)의 속성을 설정함
			
			// 1. 필드에 FormatterAttr 어노테이션이 있는 지 확인함
			FormatterAttr attrAnnotation = field.getAnnotation(FormatterAttr.class);
			if(attrAnnotation == null) {
				continue;
			}
			
			// 2. 필드의 Type(Class)과 일치하는 setter method가 있는지 확인
			//    setter 메소드는 FormatterAttrSetter 어노테이션이 선언된 메소드임
			Method setterMethod = this.getAttrSetters().get(field.getType());
			if(setterMethod == null) {
				throw new Exception("setter method is not found.:" + field.getType());
			}
			
			// 3. Formatter 객체의 필드 setter 메소드를 가져오기 위해,
			//    PropertyDescriptor를 생성함
			PropertyDescriptor pd = new PropertyDescriptor(field.getName(), formatter.getClass());
			
			// 4. FormatterAttr 어노테이션에 지정된 xml tag의 속성(attr)을 가져옴
			String attrValue = attributes.getValue(attrAnnotation.name());
			
			// 4.1 필수여부(mandatory)를 확인하여,
			//     필수인데 값이 없을 경우 오류 발생함
			if(attrValue == null || attrValue.trim().equals(""))  {
				if(attrAnnotation.mandatory() == true) {
					throw new Exception(attrAnnotation.name() + " is not set");
				} else {
					continue;
				}
			}
			
			// 5. setter 메소드를 호출하여 formatter에 속성을 설정함
			//    setter 메소드는 필드의 type(class)에 따라 속성 값을 변환하여,
			//    formatter 객체의 settter 메소드를 호출함
			setterMethod.invoke(null, formatter, pd.getWriteMethod(), attrValue);

		} // End of for
	}

}
