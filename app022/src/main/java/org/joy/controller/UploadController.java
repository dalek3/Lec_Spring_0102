package org.joy.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.joy.util.MimeMediaUtils;
import org.joy.util.UploadFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadController {
	
	private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

	//...527p.servlet-context.xml에 설정한 파일경로.
	@Resource(name = "uploadPath")
	private String uploadPath;

	@RequestMapping(value = "/uploadForm", method = RequestMethod.GET)
	public void uploadForm() {

		logger.info("uploadForm :: GET called...");
		
	}	

	//...524p.	
	@RequestMapping(value = "/uploadForm", method = RequestMethod.POST)
	public String uploadForm(MultipartFile file, Model model) throws Exception {

		/*
		 * ...523p.jsp 파일 :
		      <%@ page contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%> 필요함.
		   ...525p.web.xml : UTF-8 필터 설정.
		 */
		logger.info("originalName: " + file.getOriginalFilename());
		logger.info("size: " + file.getSize());
		logger.info("contentType: " + file.getContentType());//...파일의 MIME타입등의 정보.
	
		String savedName = uploadFile(file.getOriginalFilename(), file.getBytes());
	
		model.addAttribute("savedName", savedName);
	
		return "uploadResult";
	}	
	
	//...528p.
	private String uploadFile(String originalFilename, byte[] fileData) throws Exception {
		
		//...UUID는 중복되지 않는 고유한 키값을 설정할때 사용함.
		//   파일업로드에서 주의할 점은, 동일한 경로에 동일한 이름의 파일을 업로드하는 것.
		//   UUID와 같이 (거의)고유한 값을 생성해서 처리하면 해결할 수 있음.
		UUID uid = UUID.randomUUID();

		String savedName = uid.toString() + "_" + originalFilename;

		File target = new File(uploadPath, savedName);

		//...실제 파일처리는 스프링에서 제공하는 FileCopyUtils를 이용함.
		//   FileCopyUtils는 import org.springframework.util.FileCopyUtils패키지에 설정된 클래스임.
		//   파일 데이터를 파일로 처리하거나, 복사하는 등의 작업에 유용하게 사용될 수 있음.
		//   org.springframework.util.FileCopyUtils 패키지는 개발자가 개발하면서 필요한 여러 종류의 
		//   클래스를 제공함. 
		FileCopyUtils.copy(fileData, target);

		return savedName;

	}
	
	//...536p.
	@RequestMapping(value = "/uploadAjax", method = RequestMethod.GET)
	public void uploadAjax() {
	}
	
	/*
	 * ...544p.
	 * @RequestMapping::produces = "text/plain;charset=UTF-8"는 한국어를 정상적으로
	 * 전송하기 위한 설정임.
	 * https://docs.spring.io/spring-framework/docs/4.3.4.RELEASE/javadoc-api/index.html?org/springframework/web/bind/annotation/RequestMapping.html
	 * 
	 */
	@ResponseBody
	@RequestMapping(value ="/uploadAjax", method=RequestMethod.POST, 
		  			produces = "text/plain;charset=UTF-8")
	public ResponseEntity<String> uploadAjax(MultipartFile file)throws Exception{

		logger.info("originalName: " + file.getOriginalFilename());
		logger.info("size: " + file.getSize());
		logger.info("contentType: " + file.getContentType());//...파일의 MIME타입등의 정보.
	
	
		/*
		 * ...544p.
		 * ...HttpStatus.CREATED는 원하는 리소스가 정상적으로 생성되었다는 상태코드임.
		 * ...HttpStatus.OK를 이용해도 무방함.
		 */
		//return new ResponseEntity<>(file.getOriginalFilename(), HttpStatus.CREATED); //...559p.주석처리.
	    return new ResponseEntity<>(UploadFileUtils.uploadFile(uploadPath, 
								    	                	    file.getOriginalFilename(), 
								    	                	    file.getBytes()), 
	    	          				HttpStatus.CREATED);
		
	}	
	
	/*
	 * ...563p.
	 * displayFile()은 파라미터로 브라우저에서 전송받기를 원하는 파일이름을 받음.
	 * 파일의 이름은 '/년/월/일/파일명'의 형태로 받음.
	 * displayFile()은 반환형이 ResponseEntity<byte[]>이고, 결과는 실제로 파일의
	 * 데이터가 됨.
	 * @ResponseBody를 이용해서 byte[]데이터가 그대로 전송될 것을 명시함.
	 * ex) http://localhost:8080/displayFile?fileName=/2018/08/06/333a0e97-8f5d-429e-99cf-27de073e6552_%EC%88%98%EC%97%85%EC%B0%B8%EC%A1%B0.pptx
	 * ex) http://localhost:8080/displayFile?fileName=/2018/08/06/d995e94e-9605-4ade-a324-d5f786917d8e_%EB%B8%94%EB%A3%A8%ED%88%AC%EC%8A%A4_%EB%AF%B8%EB%8B%88%ED%82%A4%EB%B3%B4%EB%93%9C.jpg
	 */
	@ResponseBody
	@RequestMapping("/displayFile")
	public ResponseEntity<byte[]>  displayFile(String fileName)throws Exception{

		InputStream in = null; 
		ResponseEntity<byte[]> entity = null;

		logger.info("displayFile :: fileName : " + fileName);

		try{
			String formatName = fileName.substring(fileName.lastIndexOf(".")+1);
	
			//...565p.파일이름에서 확장자를 추출하고, 이미지 타입의 파일인 경우
			//적절한 MIME타입을 지정함.
			//브라우저는 이 MIME타입을 보고 사용자에게 자동으로 다운로드 창을 열어줌.			
			MediaType mType = MimeMediaUtils.getMediaType(formatName);
	
			HttpHeaders headers = new HttpHeaders();
	
			in = new FileInputStream(uploadPath+fileName);
			logger.info("in : " + in);
	
			if(mType != null){
				headers.setContentType(mType);
				
			}else{		
				fileName = fileName.substring(fileName.indexOf("_")+1);
				
				//...565p.
				//이미지가 아닌 경우, MIME타입을 다운로드 용으로 사용되는
				//'application/octet-stream'으로 지정함.
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

				//...565p.
				//다운로드 할 때 사용자에게 보이는 파일의 이름이므로 한글처리를 해서 전송함.
				//한글 파일의 경우, 다운로드하면 파일의 이름이 깨져서 나오기 때문에 반드시
				//인코딩 처리를 할 필요가 있음.
				headers.add("Content-Disposition", 
							"attachment; filename=\""
							+ new String(fileName.getBytes("UTF-8"), "ISO-8859-1")
							+ "\"");
			}
	
			//...565p.
			//실제로 데이터를 읽는 부분은 commons라이브러리의 기능을 활용해서
			//대상 파일에서 데이터를 읽어내는 IOUtils.toByteArray()임.
			//결과는 'displayFile?fileName=년/월/일/파일명'을 호출해서 확인할 수 있음.
			entity = new ResponseEntity<byte[]>(IOUtils.toByteArray(in), 
												headers, 
												HttpStatus.CREATED);
			
		}catch(Exception e){
			e.printStackTrace();
			entity = new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
			
		}finally{
			in.close();
			
		}
		
		return entity;    
	}

	/*
	 * ...578p.
	 * 화면에서 'ⓧ'를 선택해서 첨부파일을 삭제하면, 실제 저장 경로에서도 파일이 삭제됨.
	 * 이미지파일은 원본파일을 먼저 삭제하고, 이후 썸네일 파일을 삭제함.
	 */
	@ResponseBody
	@RequestMapping(value="/deleteFile", method=RequestMethod.POST)
	public ResponseEntity<String> deleteFile(String fileName){

		logger.info("delete file: "+ fileName);

		String formatName = fileName.substring(fileName.lastIndexOf(".")+1);

		MediaType mType = MimeMediaUtils.getMediaType(formatName);

		if(mType != null){
			String front = fileName.substring(0,12);
			String end = fileName.substring(14);
			String fName = (front+end).replace('/', File.separatorChar);
			logger.info("delete file2: "+ fName);
			new File(uploadPath + (front+end).replace('/', File.separatorChar)).delete();
		}

		new File(uploadPath + fileName.replace('/', File.separatorChar)).delete();

		return new ResponseEntity<String>("deleted", HttpStatus.OK);
	}  

}
