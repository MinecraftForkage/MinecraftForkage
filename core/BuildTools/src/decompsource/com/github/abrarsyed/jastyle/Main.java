/**
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   jAstyle library includes in most of its parts translated C++ code originally
 *   developed by Jim Pattee and Tal Davidson for the Artistic Style project.
 *
 *   Copyright (C) 2009 by Hector Suarez Barenca http://barenca.net
 *   Copyright (C) 2013 by Abrar Syed <sacabrarsyed@gmail.com>
 *   Copyright (C) 2006-2008 by Jim Pattee <jimp03@email.com>
 *   Copyright (C) 1998-2002 by Tal Davidson
 *   <http://www.gnu.org/licenses/lgpl-3.0.html>
 *
 *   This file is a part of jAstyle library - an indentation and
 *   reformatting library for C, C++, C# and Java source files.
 *   <http://jastyle.sourceforge.net>
 *
 *   jAstyle is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   jAstyle is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with jAstyle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

package decompsource.com.github.abrarsyed.jastyle;

import java.io.*;
import java.util.ArrayList;

public class Main
{
    private final static int EXIT_SUCCESS = 0;
    private final static int EXIT_FAILURE = -1;
    private final static String JASTYLE_VERSION = findVersion("version.txt");
    private static boolean recursive = false;
    private static File optionsFile = null;
    public static ArrayList<String> errors = new ArrayList<String>();

    private final static void printVersion()
    {
        System.out.println("\n                                 jAstyle " + JASTYLE_VERSION);
        System.out.println("                         Maintained by: AbrarSyed\n");
    }

    private final static void printHelp()
    {
        printVersion();
        printText("help.txt", new PrintWriter(System.out));

    }

    private final static String findVersion(String filename)
    {
        StringWriter writer = new StringWriter();
        printText(filename, new PrintWriter(writer));
        return writer.toString();
    }

    private final static void printText(String filename, PrintWriter out)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream(filename)));
        String line;
        try
        {
            do
            {
                line = reader.readLine();
                if (line != null)
                {
                    out.println(line);
                }
            } while (line != null);
            out.flush();
            reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // /**
    // * LINUX function to resolve wildcards and recurse into sub directories.
    // * The fileName vector is filled with the path and names of files to
    // process.
    // *
    // * @param directory The path of the directory to be processed.
    // * @param wildcard The wildcard to be processed (e.g. *.cpp).
    // * @param fileName An empty vector which will be filled with the path and
    // names of files to process.
    // */
    // void getFileNames( String directory, String wildcard, List<String>
    // fileName)
    // {
    // dirent *entry; // entry from readdir()
    // stat statbuf; // entry from stat()
    // vector<String> subDirectory; // sub directories of this directory
    //
    // // errno is defined in <errno.h> and is set for errors in opendir,
    // readdir, or stat
    // errno = 0;
    //
    // DIR *dp = opendir(directory);
    // if (errno)
    // error("Cannot open directory", directory);
    //
    // // save the first fileName entry for this recursion
    // unsigned firstEntry = fileName.size();
    //
    // // save files and sub directories
    // while ((entry = readdir(dp)) != null)
    // {
    // // get file status
    // String entryFilepath = directory + FILE_SEPARATOR + entry->d_name;
    // stat(entryFilepath, statbuf);
    // if (errno)
    // {
    // if (errno == EOVERFLOW) // file over 2 GB is OK
    // {
    // errno = 0;
    // continue;
    // }
    // perror("errno message");
    // error("Error getting file status in directory", directory);
    // }
    // // skip hidden or read only
    // if (entry->d_name[0] == '.' || !(statbuf.st_mode & S_IWUSR))
    // continue;
    // // if a sub directory and recursive, save sub directory
    // if (S_ISDIR(statbuf.st_mode) && isRecursive)
    // {
    // if (isPathExclued(entryFilepath))
    // System.out.println("exclude " +
    // entryFilepath.substr(mainDirectoryLength));
    // else
    // subDirectory.push_back(entryFilepath);
    // continue;
    // }
    //
    // // if a file, save file name
    // if (S_ISREG(statbuf.st_mode))
    // {
    // // check exclude before wildcmp to avoid "unmatched exclude" error
    // boolean isExcluded = isPathExclued(entryFilepath);
    // // save file name if wildcard match
    // if (wildcmp(wildcard, entry->d_name))
    // {
    // if (isExcluded)
    // System.out.println("exclude " +
    // entryFilepath.substr(mainDirectoryLength));
    // else
    // fileName.push_back(entryFilepath);
    // }
    // }
    // }
    // closedir(dp);
    //
    // if (errno)
    // {
    // perror("errno message");
    // error("Error reading directory", directory);
    // }
    //
    // // sort the current entries for fileName
    // if (firstEntry < fileName.size())
    // sort(fileName[firstEntry], fileName[fileName.size()]);
    //
    // // recurse into sub directories
    // // if not doing recursive, subDirectory is empty
    // if (subDirectory.size() > 1)
    // sort(subDirectory.begin(), subDirectory.end());
    // for (unsigned i = 0; i < subDirectory.size(); i++)
    // {
    // getFileNames(subDirectory[i], wildcard, fileName);
    // }
    //
    // return;
    // }
    //
    //
    // void preserveFileDate(String oldFileName, String newFileName)
    // {
    // stat stBuf;
    // boolean statErr = false;
    // if (stat (oldFileName, stBuf) == -1)
    // statErr = true;
    // else
    // {
    // utimbuf outBuf;
    // outBuf.actime = stBuf.st_atime;
    // // add 2 so 'make' will recoginze a change
    // // Visual Studio 2008 needs 2
    // outBuf.modtime = stBuf.st_mtime + 2;
    // if (utime (newFileName, outBuf) == -1)
    // statErr = true;
    // }
    // if (statErr)
    // System.out.println("    Could not preserve file date");
    // }

    // // process a command-line file path, including wildcards
    // void processFilePath(String filePath, ASFormatter formatter)
    // {
    // vector<String> fileName; // files to be processed including path
    // String targetDirectory; // path to the directory being processed
    // String targetFilename; // file name being processed
    //
    // // standardize the file separators
    // standardizePath(filePath);
    //
    // // separate directory and file name
    // size_t separator = filePath.find_last_of(FILE_SEPARATOR);
    // if (separator == -1)
    // {
    // // if no directory is present, use the currently active directory
    // targetDirectory = getCurrentDirectory(filePath);
    // targetFilename = filePath;
    // mainDirectoryLength = targetDirectory.length() + 1; // +1 includes
    // trailing separator
    // }
    // else
    // {
    // targetDirectory = filePath.substr(0, separator);
    // targetFilename = filePath.substr(separator+1);
    // mainDirectoryLength = targetDirectory.length() + 1; // +1 includes
    // trailing separator
    // }
    //
    // if (targetFilename.length() == 0)
    // error("Missing filename in", filePath);
    //
    // // check filename for wildcards
    // hasWildcard = false;
    // if (targetFilename.find_first_of( "*?") != -1)
    // hasWildcard = true;
    //
    // // clear exclude hits vector
    // for (size_t ix = 0; ix < excludeHitsVector.size(); ix++)
    // excludeHitsVector[ix] = false;
    //
    // // display directory name for wildcard processing
    // if (hasWildcard && ! isQuiet)
    // {
    // System.out.println("--------------------------------------------------");
    // System.out.println("directory " + targetDirectory + FILE_SEPARATOR +
    // targetFilename;
    // }
    //
    // // create a vector of paths and file names to process
    // if (hasWildcard || isRecursive)
    // getFileNames(targetDirectory, targetFilename, fileName);
    // else
    // fileName.push_back(filePath);
    //
    // if (hasWildcard && ! isQuiet)
    // System.out.println("--------------------------------------------------");
    //
    // // check for unprocessed excludes
    // boolean excludeErr = false;
    // for (size_t ix = 0; ix < excludeHitsVector.size(); ix++)
    // {
    // if (excludeHitsVector[ix] == false)
    // {
    // System.out.println("Unmatched exclude " + excludeVector[ix]);
    // excludeErr = true;
    // }
    // }
    // if (excludeErr)
    // exit(EXIT_FAILURE);
    //
    // // check if files were found (probably an input error if not)
    // if (fileName.size() == 0)
    // System.out.println("No file to process " + filePath);
    //
    // // loop thru fileName vector to format the files
    // for (size_t j = 0; j < fileName.size(); j++)
    // {
    // // format the file
    // boolean isFormatted = formatFile(fileName[j], formatter);
    //
    // // remove targetDirectory from filename if required
    // String displayName;
    // if (hasWildcard)
    // displayName = fileName[j].substr(targetDirectory.length() + 1);
    // else
    // displayName = fileName[j];
    //
    // if (isFormatted)
    // {
    // filesFormatted++;
    // if (!isQuiet)
    // System.out.println("formatted  " + displayName);
    // }
    // else
    // {
    // filesUnchanged++;
    // if (!isQuiet && !isFormattedOnly)
    // System.out.println("unchanged* " + displayName);
    // }
    // }
    // }

    //
    // , optionsVector, and fileOptionsVector

    // // rename a file and check for an error
    // void renameFile(String oldFileName, String newFileName, String errMsg)
    // {
    // rename(oldFileName, newFileName);
    // // if file still exists the remove needs more time - retry
    // if (errno == EEXIST)
    // {
    // errno = 0;
    // waitForRemove(newFileName);
    // rename(oldFileName, newFileName);
    // }
    // if (errno)
    // {
    // perror("errno message");
    // error(errMsg, oldFileName);
    // }
    // }

    /**
     * Not supported options:<br>
     * <p/>
     * <p/>
     * <pre>
     * --suffix=####
     * --suffix=none / -n
     * --exclude=####
     * --errors-to-stdout / -X
     * --preserve-date / -Z
     * --verbose / -v
     * --formatted / -Q
     * --quiet / -q
     * </pre>
     *
     * @param cliArgs
     * @throws IOException
     */
    public static void main(String[] cliArgs) throws IOException
    {
        // intial catch for no options
        if (cliArgs.length < 1)
        {
            printHelp();
            System.exit(EXIT_SUCCESS);
        }

        System.out.println("Parsing options...");

        // clear errors list.
        errors.clear();

        // convert to list so we can add and remove stuff easily.
        ArrayList<String> args = convertList(cliArgs);

        // parse non-formatter stuff    changes the args list too.
        ArrayList<String> filenames = parseConsoleOptions(args);

        ASFormatter formatter = new ASFormatter();
        OptParser parser = new OptParser(formatter);

        // check options file first.. since they are overwrite by the other options.
        if (optionsFile != null)
        {
            // get the errors...
            ArrayList<String> parsedErrors = parser.parseOptionFile(optionsFile);

            // catch and output the IO error.
            if (parsedErrors == null)
            {
                errors.add("something went wrong reading options file : " + optionsFile);
            }

            // and add them to the actual errors list.
            for (String e : parsedErrors)
            {
                System.err.println(e + " is not a supported config file option, continuing...");
            }
        }

        // now the normal options.
        for (String opt : args)
        {
            try
            {
                parser.parseOption(opt);
            }
            catch (MalformedOptionException e)
            {
                errors.add("option " + opt + " is not a valid option");
            }
        }

        // grab the errors before anything special happens.

        if (errors.size() > 0)
        {
            for (String error : errors)
            {
                System.err.println(error);
            }
            System.exit(EXIT_FAILURE);
        }

        // now we make sure there are no conflicting messages.
        formatter.fixOptionVariableConflicts();

        System.out.println("Parsing file names...");

        // now we go through the filenames and parse them into files.
        ArrayList<File> files = parseFileNames(filenames, recursive);

        System.out.println("Formatting files.");

        // now we format the collected files
        boolean worked;
        for (File currFile : files)
        {
            System.out.println("Converting " + currFile.getAbsolutePath() + " ...\n");
            worked = formatter.formatFile(currFile);
            if (!worked)
            {
                System.out.println("Error formatting file " + currFile.getAbsolutePath() + " ...\n");
            }
        }

        System.out.println("Complete");

        System.exit(EXIT_SUCCESS);
    }

    /**
     * This method parses the CommandLine options that have no bearing on the actual formatting, and will ONLY be parsed when coming from the command line.
     * The OptParser is reserved for only changing formatter options.
     *
     * @param args A list with all of the parsed options removed. leaving only things for the OptParser to go through.
     * @return A list of FileNames to be parsed into actual paths
     */
    public static ArrayList<String> parseConsoleOptions(ArrayList<String> args)
    {
        ArrayList<String> filenames = new ArrayList<String>();
        ArrayList<String> toRemove = new ArrayList<String>();
        String temp;
        // used normal loop so I can remove elements
        for (String arg : args)
        {
            if (arg.startsWith("--"))
            {
                // LONG CLI options
                temp = arg.substring(2);
                if (temp.equals("recursive"))
                {
                    recursive = true;
                }
                else if (temp.equals("version"))
                {
                    printVersion();
                    System.exit(EXIT_SUCCESS);
                }
                else if (temp.equals("help"))
                {
                    printHelp();
                    System.exit(EXIT_SUCCESS);
                }
                else if (temp.startsWith("options="))
                {
                    temp = temp.substring(8);

                    if (temp.equals("none"))
                    {
                        optionsFile = null;
                    }
                    else
                    {
                        optionsFile = new File(temp);
                        if (!optionsFile.exists())
                        {
                            errors.add("the file " + temp + " could not be found.");
                        }
                    }
                }
                else
                {
                    // other options that havent been parsed yet.
                    continue;
                }

                toRemove.add(arg);
            }
            else if (arg.startsWith("-"))
            {
                if (arg.length() > 2)
                {
                    // leave real error checking to the OptParser
                    //errors.add(arg + " is not a supported command-line option.");
                    continue;
                }

                // SHORT CLI options

                switch (arg.charAt(1))
                {
                    case 'r':
                    case 'R':
                        recursive = true;
                        toRemove.add(arg);  // only one that doesn't terminate the program.
                        break;
                    case 'V':
                        printVersion();
                        System.exit(EXIT_SUCCESS);
                    case 'h':
                    case '?':
                        printHelp();
                        System.exit(EXIT_SUCCESS);
                }
            }
            else
            {
                // isn't a short or a long. good to add as a file.
                filenames.add(arg);
                // remove these from the list so they arn't flagged in the OptParser.
                toRemove.add(arg);
            }
        }

        args.removeAll(toRemove);

        return filenames;
    }

    /**
     * Parses filenames and patterns into a list of existing files.
     *
     * @param filenames List of filenames to parse.
     * @param recursive Whether or not to search recursively
     * @return a list of existing files.
     */
    private static ArrayList<File> parseFileNames(ArrayList<String> filenames, boolean recursive)
    {
        ArrayList<File> files = new ArrayList<File>();
        FilenameFilter filter = null;
        File temp;
        String dir, name;

        // collect potential files
        for (String filepath : filenames)
        {
            int index = filepath.lastIndexOf(File.separatorChar);

            // no slash? directory is here.
            if (index < 0)
            {
                dir = ".";
                name = filepath;
            }
            else
            {
                // slash index is somehow larger than string??? this isnt possible.....
                if (index >= filepath.length())
                {
                    System.err.println("The filename " + filepath + " is invalid");
                    System.exit(EXIT_FAILURE);
                }

                // split the dir and the file name into different strings for parsing later
                dir = filepath.substring(0, index);
                name = filepath.substring(index + 1);
            }

            // filename with wildcard
            if (name.indexOf('*') != -1 || name.indexOf('?') != -1)
            {
                // set teh filter
                filter = new FileWildcardFilter(name);

                // set teh searching file to the directory
                temp = new File(dir);
            }

            // only possible with a trailing slash
            // pointing to a directory no?
            else if (name.isEmpty())
            {
                temp = new File(dir);
            }

            // straight up filename
            else
            {
                temp = new File(dir, name);
            }

            // collect the files
            files.addAll(collectFiles(temp, filter, recursive));

            // clear filter status.
            filter = null;
        }

        return files;
    }

    private static <T> ArrayList<T> convertList(T[] array)
    {
        ArrayList<T> list = new ArrayList<T>();
        for (T obj : array)
        {
            list.add(obj);
        }
        return list;
    }

    /**
     * @param dir     Directory to search in
     * @param filter  Filter to apply to files. May be null to accept all the file names.
     * @param recurse If subdirectopries should be recursed through or not.
     * @return ArrayList of accepted files. Will never be null, only an empty list.
     */
    public static ArrayList<File> collectFiles(File dir, FilenameFilter filter, boolean recurse)
    {
        ArrayList<File> files = new ArrayList<File>();

        // check if supplied directory is a file itself.
        if (dir.isFile() && (filter == null || filter.accept(dir, dir.getName())))
        {
            files.add(dir);
            return files;
        }

        // otherwise.. recurse and search for files.
        for (File file : dir.listFiles())
        {
            // if its a file, AND .....
            // if the filter is null, add it.
            // if filter accepts the file add it.
            if (file.isFile() && (filter == null || filter.accept(dir, file.getName())))
            {
                files.add(file);
            }

            // its a directory and recursing is enabled.. recurse.
            if (file.isDirectory() && recurse)
            {
                files.addAll(collectFiles(file, filter, recurse));
            }
        }

        return files;
    }
}
