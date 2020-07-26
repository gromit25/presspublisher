package com.gromit25.presspublisher.formatter.excel;

import java.nio.charset.Charset;

import com.gromit25.presspublisher.common.PublisherUtil;
import com.gromit25.presspublisher.evaluator.ValueContainer;
import com.gromit25.presspublisher.formatter.FormatterException;
import com.gromit25.presspublisher.formatter.flow.BasicFlowFormatter;

/**
 * Excel의 Cell 출력용 Formatter의 추상 클래스
 * 
 * @author jmsohn
 */
public abstract class AbstractExcelFormatter extends BasicFlowFormatter {

	/**
	 * excel에 출력작업 수행
	 * @param copy 출력 대상 WorksheetFormatter
	 * @param charset 출력시 사용할 character set
	 * @param values value container
	 */
	protected abstract void formatExcel(WorksheetFormatter copy, Charset charset, ValueContainer values) throws FormatterException;
	
	@Override
	public void format(Object copyObj, Charset charset, ValueContainer values) throws FormatterException {

		try {
			WorksheetFormatter copy = PublisherUtil.cast(copyObj, WorksheetFormatter.class);
			this.formatExcel(copy, charset, values);
		} catch(FormatterException fex) {
			throw fex;
		} catch(Exception ex) {
			throw new FormatterException(this, ex);
		}
		
	}

}
