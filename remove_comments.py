#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
代码注释去除器
支持多种编程语言的注释去除，包括：
- C/C++ (// 和 /* */)
- Python (# 和 """ """ / ''' ''')
- Java (// 和 /* */)
- JavaScript (// 和 /* */)
- HTML (<!-- -->)
"""

import re
import os
import sys
from typing import Dict, List, Tuple

class CommentRemover:
    def __init__(self):
        # 定义不同语言的注释模式
        self.comment_patterns = {
            'c': {
                'single_line': r'//.*?$',
                'multi_line': r'/\*.*?\*/',
                'multi_line_flags': re.DOTALL | re.MULTILINE
            },
            'cpp': {
                'single_line': r'//.*?$',
                'multi_line': r'/\*.*?\*/',
                'multi_line_flags': re.DOTALL | re.MULTILINE
            },
            'python': {
                'single_line': r'#.*?$',
                'multi_line': r'""".*?"""|\'\'\'.*?\'\'\'',
                'multi_line_flags': re.DOTALL | re.MULTILINE
            },
            'java': {
                'single_line': r'//.*?$',
                'multi_line': r'/\*.*?\*/',
                'multi_line_flags': re.DOTALL | re.MULTILINE
            },
            'javascript': {
                'single_line': r'//.*?$',
                'multi_line': r'/\*.*?\*/',
                'multi_line_flags': re.DOTALL | re.MULTILINE
            },
            'html': {
                'single_line': r'',
                'multi_line': r'<!--.*?-->',
                'multi_line_flags': re.DOTALL
            }
        }
        
        # 文件扩展名到语言的映射
        self.extension_map = {
            '.c': 'c',
            '.cpp': 'cpp',
            '.cxx': 'cpp',
            '.cc': 'cpp',
            '.h': 'c',
            '.hpp': 'cpp',
            '.py': 'python',
            '.java': 'java',
            '.js': 'javascript',
            '.html': 'html',
            '.htm': 'html'
        }
    
    def detect_language(self, filename: str) -> str:
        """根据文件扩展名检测编程语言"""
        _, ext = os.path.splitext(filename.lower())
        return self.extension_map.get(ext, 'unknown')
    
    def remove_comments_from_content(self, content: str, language: str) -> str:
        """从内容中移除注释"""
        if language not in self.comment_patterns:
            print(f"警告：不支持的编程语言 '{language}'，跳过注释移除")
            return content
        
        patterns = self.comment_patterns[language]
        result = content
        
        # 移除多行注释
        if 'multi_line' in patterns and patterns['multi_line']:
            result = re.sub(patterns['multi_line'], '', result, flags=patterns['multi_line_flags'])
        
        # 移除单行注释
        if 'single_line' in patterns and patterns['single_line']:
            result = re.sub(patterns['single_line'], '', result, flags=patterns['multi_line_flags'])
        
        # 清理空行和多余空格
        lines = result.split('\n')
        cleaned_lines = []
        for line in lines:
            stripped = line.rstrip()
            if stripped or line.strip() == '':  # 保留空行以保持代码结构
                cleaned_lines.append(stripped)
        
        return '\n'.join(cleaned_lines)
    
    def process_file(self, filepath: str, output_path: str = None) -> bool:
        """处理单个文件"""
        try:
            # 检测编程语言
            language = self.detect_language(filepath)
            if language == 'unknown':
                print(f"跳过文件 {filepath}：不支持的文件类型")
                return False
            
            # 读取文件内容
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 移除注释
            cleaned_content = self.remove_comments_from_content(content, language)
            
            # 确定输出路径
            if output_path is None:
                base_name = os.path.basename(filepath)
                name, ext = os.path.splitext(base_name)
                output_path = os.path.join(os.path.dirname(filepath), f"{name}_no_comments{ext}")
            
            # 写入清理后的内容
            with open(output_path, 'w', encoding='utf-8') as f:
                f.write(cleaned_content)
            
            print(f"成功处理文件：{filepath} -> {output_path}")
            return True
            
        except Exception as e:
            print(f"处理文件 {filepath} 时出错：{str(e)}")
            return False
    
    def process_directory(self, dirpath: str, output_dir: str = None, recursive: bool = True) -> int:
        """处理目录中的所有支持文件"""
        processed_count = 0
        
        try:
            if output_dir and not os.path.exists(output_dir):
                os.makedirs(output_dir)
            
            for root, dirs, files in os.walk(dirpath):
                if not recursive:
                    dirs.clear()  # 只处理当前目录
                
                for file in files:
                    filepath = os.path.join(root, file)
                    
                    # 检查文件扩展名是否支持
                    _, ext = os.path.splitext(file.lower())
                    if ext in self.extension_map:
                        try:
                            # 确定输出路径
                            if output_dir:
                                rel_path = os.path.relpath(filepath, dirpath)
                                output_path = os.path.join(output_dir, rel_path)
                                output_subdir = os.path.dirname(output_path)
                                if not os.path.exists(output_subdir):
                                    os.makedirs(output_subdir)
                            else:
                                output_path = None
                            
                            if self.process_file(filepath, output_path):
                                processed_count += 1
                                
                        except Exception as e:
                            print(f"处理文件 {filepath} 时出错：{str(e)}")
                            continue
        
        except Exception as e:
            print(f"处理目录 {dirpath} 时出错：{str(e)}")
        
        return processed_count
    
    def get_supported_extensions(self) -> List[str]:
        """获取支持的文件扩展名列表"""
        return list(self.extension_map.keys())

def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='代码注释移除工具')
    parser.add_argument('input', help='输入文件或目录路径')
    parser.add_argument('-o', '--output', help='输出文件或目录路径')
    parser.add_argument('-r', '--recursive', action='store_true', default=True, 
                       help='递归处理子目录（默认开启）')
    parser.add_argument('-l', '--language', help='指定编程语言（自动检测如果未指定）')
    parser.add_argument('-e', '--extensions', action='store_true', 
                       help='显示支持的文件扩展名')
    parser.add_argument('-v', '--verbose', action='store_true', 
                       help='显示详细处理信息')
    
    args = parser.parse_args()
    
    remover = CommentRemover()
    
    # 显示支持的扩展名
    if args.extensions:
        print("支持的文件扩展名：")
        for ext in sorted(remover.get_supported_extensions()):
            print(f"  {ext}")
        return
    
    # 检查输入路径
    if not os.path.exists(args.input):
        print(f"错误：输入路径 '{args.input}' 不存在")
        sys.exit(1)
    
    # 处理文件或目录
    if os.path.isfile(args.input):
        # 处理单个文件
        if args.language:
            language = args.language.lower()
        else:
            language = remover.detect_language(args.input)
        
        success = remover.process_file(args.input, args.output)
        if success:
            print(f"文件处理完成！")
        else:
            sys.exit(1)
    
    elif os.path.isdir(args.input):
        # 处理目录
        print(f"开始处理目录：{args.input}")
        processed = remover.process_directory(args.input, args.output, args.recursive)
        print(f"处理完成！共处理了 {processed} 个文件")
    
    else:
        print(f"错误：'{args.input}' 既不是文件也不是目录")
        sys.exit(1)

if __name__ == "__main__":
    main()